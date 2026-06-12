package com.irishpubfinder.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irishpubfinder.api.exception.PlacesApiException;
import com.irishpubfinder.api.model.PlaceDetailsCache;
import com.irishpubfinder.api.model.PlaceSearchCache;
import com.irishpubfinder.api.repository.PlaceDetailsCacheRepository;
import com.irishpubfinder.api.repository.PlaceSearchCacheRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GooglePlacesService {

    private static final String API_BASE    = "https://places.googleapis.com/v1";
    private static final String PLACES_BASE = API_BASE + "/places";

    private static final String SEARCH_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location," +
            "places.rating,places.userRatingCount,places.regularOpeningHours," +
            "places.photos,places.priceLevel,places.types,places.businessStatus,nextPageToken";

    private static final String DETAILS_FIELD_MASK =
            "id,displayName,formattedAddress,location,rating,regularOpeningHours," +
            "nationalPhoneNumber,websiteUri,addressComponents,editorialSummary,reviews";

    // ~8 km in latitude degrees — the cache grid cell size (see getNearbyPubs)
    private static final double GRID_DEG = 0.072;
    private static final int PAGE_SIZE = 20;
    // Google's Text Search caps results at 3 pages (60 results)
    private static final int MAX_PAGES = 3;
    private static final int PAGE_RETRY_ATTEMPTS = 4;
    private static final long PAGE_RETRY_DELAY_MS = 1500;
    // Synthetic pagination token: "nb1.<base64url(cellKey)>.<offset>" — never collides with a Google token
    private static final String SYNTH_TOKEN_PREFIX = "nb1.";

    @Value("${google.places.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlaceSearchCacheRepository searchRepo;
    private final PlaceDetailsCacheRepository detailsRepo;
    private final ApiMetricsService metrics;

    public GooglePlacesService(PlaceSearchCacheRepository searchRepo,
                               PlaceDetailsCacheRepository detailsRepo,
                               ApiMetricsService metrics) {
        this.searchRepo = searchRepo;
        this.detailsRepo = detailsRepo;
        this.metrics = metrics;
    }

    @PostConstruct
    void validate() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "google.places.api.key is not set — start the server with GOOGLE_MAPS_API_KEY env var");
        }
        log.info("Google Places API key loaded (length={})", apiKey.length());
    }

    /**
     * Returns one 20-result page of the "irish pub" search for a location.
     *
     * On a cache miss we eagerly fetch ALL of Google's pages (max 3 / 60 results), merge
     * them into one cached result set (24h TTL), and strip Google's short-lived
     * nextPageToken. Pages are then served to the client via our own synthetic offset
     * tokens that index into the cached set — so every page after the cold fetch (including
     * every "load more") is served from cache with zero Google calls. The client treats the
     * token as opaque and always re-sends lat/lng/radius, so a synthetic token can always be
     * re-materialized if the cache entry has since expired.
     */
    public String getNearbyPubs(double lat, double lng, int radius, String pageToken) {
        // Snap the search centre to an ~8km grid so nearby users share a cache cell. The
        // snapped centre is used for BOTH the Google request and the cache key, so the cached
        // set is consistently centred (any user in the cell is within ~4km of it). Distance
        // "closest first" sorting is done client-side off the user's real GPS, so it is
        // unaffected by snapping.
        long latBucket = Math.round(lat / GRID_DEG);
        long lngBucket = Math.round(lng / GRID_DEG);
        double snLat = latBucket * GRID_DEG;
        double snLng = lngBucket * GRID_DEG;
        // "v3:" prefix isolates 8km-grid entries from legacy v2 (1km) and v1 rows
        String cellKey = String.format("v3:%d,%d,%d", latBucket, lngBucket, radius);

        // First-page request
        if (pageToken == null || pageToken.isBlank()) {
            return serveFromCacheOrMaterialize(cellKey, snLat, snLng, radius, 0);
        }

        // Synthetic offset token → serve the requested slice from cache (re-materialize if expired).
        // Use the cellKey embedded in the token, not the recomputed one, so pagination stays
        // tied to the set that issued the token even if GPS jitter crosses a cell boundary.
        SyntheticToken parsed = parseSyntheticToken(pageToken);
        if (parsed != null) {
            return serveFromCacheOrMaterialize(parsed.cellKey(), snLat, snLng, radius, parsed.offset());
        }

        // Unknown/legacy token (e.g. a real Google token from a client running pre-deploy code).
        // Fall back to the old direct passthrough; unreachable once such tokens expire (<24h).
        metrics.record(ApiMetricsService.NEARBY_SEARCH);
        return callTextSearch(snLat, snLng, radius, pageToken);
    }

    /** Serves the {@code offset} slice from the cell's cached set, materializing it on a miss. */
    private String serveFromCacheOrMaterialize(String cellKey, double lat, double lng, int radius, int offset) {
        String combined = searchRepo.findValid(cellKey, Instant.now())
                .map(cached -> {
                    metrics.record(ApiMetricsService.NEARBY_SEARCH_CACHE);
                    return cached.getResponseJson();
                })
                .orElse(null);
        if (combined == null) {
            combined = materializeCell(cellKey, lat, lng, radius);
        }
        // Propagate a Google error body unsliced so the client keeps its existing error handling
        // (slicing would turn it into a misleading empty result set).
        if (!isSuccess(combined)) {
            return combined;
        }
        return sliceResponse(combined, cellKey, offset);
    }

    /**
     * Fetches every Google page for the cell (max {@link #MAX_PAGES}), merges them into one
     * result set with no nextPageToken, upserts it into the cache (24h TTL), and returns it.
     * On a page-1 error nothing is cached and the raw error body is returned. A partial set
     * (later page failed) is still cached and served.
     */
    private String materializeCell(String cellKey, double lat, double lng, int radius) {
        metrics.record(ApiMetricsService.NEARBY_SEARCH);
        String page1 = callTextSearch(lat, lng, radius, null);
        if (!isSuccess(page1)) {
            return page1;
        }

        List<String> pages = new ArrayList<>();
        pages.add(page1);
        String nextToken = readNextPageToken(page1);
        while (nextToken != null && pages.size() < MAX_PAGES) {
            String page = fetchPageWithRetry(lat, lng, radius, nextToken);
            if (!isSuccess(page)) {
                break; // keep what we have; a partial set beats failing
            }
            pages.add(page);
            nextToken = readNextPageToken(page);
        }

        String combined = mergePages(pages);
        PlaceSearchCache entry = searchRepo.findByCellKey(cellKey).orElseGet(PlaceSearchCache::new);
        entry.setCellKey(cellKey);
        entry.setResponseJson(combined);
        entry.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        searchRepo.save(entry);
        return combined;
    }

    /**
     * Fetches a follow-up page, retrying on Google's transient "page token not ready" error.
     * Only ever runs on a cold cache (once per cell per 24h), so the short blocking sleep is
     * acceptable.
     */
    private String fetchPageWithRetry(double lat, double lng, int radius, String pageToken) {
        String json = "";
        for (int attempt = 0; attempt < PAGE_RETRY_ATTEMPTS; attempt++) {
            json = callTextSearch(lat, lng, radius, pageToken);
            if (isSuccess(json)) {
                return json;
            }
            try {
                Thread.sleep(PAGE_RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return json;
    }

    public String getPlaceDetails(String placeId, String sessionToken) {
        // "v2:" prefix isolates new-API cached responses from legacy ones
        String cacheKey = "v2:" + placeId;
        return detailsRepo.findValid(cacheKey, Instant.now())
                .map(cached -> {
                    metrics.record(ApiMetricsService.PLACE_DETAILS_CACHE);
                    return cached.getResponseJson();
                })
                .orElseGet(() -> {
                    metrics.record(ApiMetricsService.PLACE_DETAILS);
                    String json = callPlaceDetails(placeId, sessionToken);
                    if (isSuccess(json)) {
                        PlaceDetailsCache entry = detailsRepo.findByPlaceId(cacheKey)
                                .orElse(new PlaceDetailsCache());
                        entry.setPlaceId(cacheKey);
                        entry.setResponseJson(json);
                        entry.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
                        detailsRepo.save(entry);
                    }
                    return json;
                });
    }

    /**
     * Fetches raw photo bytes. photoName is the full resource path returned by the new
     * Places API, e.g. "places/{id}/photos/{token}". Google responds with a redirect to
     * the CDN URL; RestTemplate follows it automatically.
     */
    public Object[] getPhotoBytes(String photoName, int maxwidth) {
        URI uri = URI.create(API_BASE + "/" + photoName + "/media?maxWidthPx=" + maxwidth + "&key=" + apiKey);
        log.info("Google Places Photo → fetching (name={})", photoName);
        try {
            ResponseEntity<byte[]> resp =
                    restTemplate.exchange(uri, HttpMethod.GET, null, byte[].class);
            byte[] bytes = resp.getBody();
            if (bytes == null || bytes.length == 0) {
                throw new PlacesApiException("Empty photo body from Google");
            }
            String ct = resp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            return new Object[]{ bytes, ct != null ? ct : "image/jpeg" };
        } catch (RestClientException ex) {
            throw new PlacesApiException("Google Places Photo unavailable: " + ex.getMessage());
        }
    }

    /**
     * Resolves a CURRENT first-photo resource name for a place via Place Details. Used to
     * recover when a stored photo token has gone stale (e.g. an old saved visit) — the photo
     * token in a saved URL expires, but the placeId is permanent. Returns null if the place
     * has no photo or the lookup fails.
     */
    public String getFirstPhotoName(String placeId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", "id,photos");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            metrics.record(ApiMetricsService.PLACE_DETAILS);
            ResponseEntity<String> resp = restTemplate.exchange(
                    URI.create(PLACES_BASE + "/" + placeId), HttpMethod.GET, entity, String.class);
            JsonNode name = objectMapper.readTree(resp.getBody() != null ? resp.getBody() : "{}")
                    .path("photos").path(0).path("name");
            return name.isMissingNode() || name.isNull() ? null : name.asText();
        } catch (Exception ex) {
            log.warn("Could not resolve fresh photo name for place {}: {}", placeId, ex.getMessage());
            return null;
        }
    }

    public String getAutocomplete(String input, String sessionToken) {
        metrics.record(ApiMetricsService.AUTOCOMPLETE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", input);
        body.put("languageCode", "en");
        if (sessionToken != null && !sessionToken.isBlank()) {
            body.put("sessionToken", sessionToken);
        }
        return postToPlaces(":autocomplete", body, null);
    }

    private String callTextSearch(double lat, double lng, int radius, String pageToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", "irish pub");
        body.put("pageSize", 20);

        Map<String, Object> center = new LinkedHashMap<>();
        center.put("latitude", lat);
        center.put("longitude", lng);
        Map<String, Object> circle = new LinkedHashMap<>();
        circle.put("center", center);
        circle.put("radius", (double) radius);
        Map<String, Object> locationBias = new LinkedHashMap<>();
        locationBias.put("circle", circle);
        body.put("locationBias", locationBias);

        if (pageToken != null && !pageToken.isBlank()) {
            body.put("pageToken", pageToken);
        }

        return postToPlaces(":searchText", body, SEARCH_FIELD_MASK);
    }

    private String callPlaceDetails(String placeId, String sessionToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", DETAILS_FIELD_MASK);
        if (sessionToken != null && !sessionToken.isBlank()) {
            headers.set("X-Goog-Session-Token", sessionToken);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        log.info("Google Places (New) Details → placeId={}", placeId);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    URI.create(PLACES_BASE + "/" + placeId),
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            String result = resp.getBody() != null ? resp.getBody() : "{}";
            log.info("Google Places ← snippet: {}", result.substring(0, Math.min(120, result.length())));
            return result;
        } catch (RestClientException ex) {
            throw new PlacesApiException("Google Places API unavailable: " + ex.getMessage());
        }
    }

    private String postToPlaces(String pathSuffix, Map<String, Object> body, String fieldMask) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Goog-Api-Key", apiKey);
            if (fieldMask != null && !fieldMask.isBlank()) {
                headers.set("X-Goog-FieldMask", fieldMask);
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            String bodyJson = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
            log.info("Google Places (New) POST → {}{}", PLACES_BASE, pathSuffix);
            ResponseEntity<String> resp = restTemplate.exchange(
                    URI.create(PLACES_BASE + pathSuffix),
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            String result = resp.getBody() != null ? resp.getBody() : "{}";
            log.info("Google Places ← snippet: {}", result.substring(0, Math.min(120, result.length())));
            return result;
        } catch (RestClientException ex) {
            throw new PlacesApiException("Google Places API unavailable: " + ex.getMessage());
        } catch (Exception ex) {
            throw new PlacesApiException("Failed to build Places request: " + ex.getMessage());
        }
    }

    private static boolean isSuccess(String json) {
        // New API uses HTTP status codes for errors; a success body never contains "error"
        return !json.contains("\"error\"");
    }

    private String readNextPageToken(String json) {
        try {
            JsonNode token = objectMapper.readTree(json).get("nextPageToken");
            return token != null && !token.isNull() ? token.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Concatenates the {@code places} arrays of each page into one object with no token. */
    String mergePages(List<String> pageJsons) {
        ObjectNode combined = objectMapper.createObjectNode();
        ArrayNode allPlaces = combined.putArray("places");
        for (String pageJson : pageJsons) {
            try {
                JsonNode places = objectMapper.readTree(pageJson).get("places");
                if (places != null && places.isArray()) {
                    places.forEach(allPlaces::add);
                }
            } catch (Exception e) {
                log.warn("Skipping unparseable search page during merge: {}", e.getMessage());
            }
        }
        return combined.toString();
    }

    /**
     * Returns the {@code [offset, offset+PAGE_SIZE)} slice of the cached set, shaped like a
     * Google page ({@code {places:[...], nextPageToken?}}). A synthetic nextPageToken is added
     * only when more results remain.
     */
    String sliceResponse(String combinedJson, String cellKey, int offset) {
        try {
            JsonNode all = objectMapper.readTree(combinedJson).get("places");
            int total = (all != null && all.isArray()) ? all.size() : 0;

            ObjectNode out = objectMapper.createObjectNode();
            ArrayNode page = out.putArray("places");
            int end = Math.min(offset + PAGE_SIZE, total);
            for (int i = Math.max(offset, 0); i < end; i++) {
                page.add(all.get(i));
            }
            if (end < total) {
                out.put("nextPageToken", buildSyntheticToken(cellKey, end));
            }
            return out.toString();
        } catch (Exception e) {
            log.warn("Failed to slice cached search response: {}", e.getMessage());
            return combinedJson;
        }
    }

    static String buildSyntheticToken(String cellKey, int offset) {
        String encodedKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(cellKey.getBytes(StandardCharsets.UTF_8));
        return SYNTH_TOKEN_PREFIX + encodedKey + "." + offset;
    }

    /** Decoded synthetic pagination token: which cached cell, and the page offset within it. */
    record SyntheticToken(String cellKey, int offset) {}

    /** Returns the decoded token, or null if {@code token} is not a valid synthetic token. */
    static SyntheticToken parseSyntheticToken(String token) {
        if (token == null || !token.startsWith(SYNTH_TOKEN_PREFIX)) {
            return null;
        }
        String[] parts = token.split("\\.", 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            String cellKey = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int offset = Integer.parseInt(parts[2]);
            return offset >= 0 ? new SyntheticToken(cellKey, offset) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
