package com.irishpubfinder.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
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

    @Value("${google.places.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlaceSearchCacheRepository searchRepo;
    private final PlaceDetailsCacheRepository detailsRepo;

    public GooglePlacesService(PlaceSearchCacheRepository searchRepo,
                               PlaceDetailsCacheRepository detailsRepo) {
        this.searchRepo = searchRepo;
        this.detailsRepo = detailsRepo;
    }

    @PostConstruct
    void validate() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "google.places.api.key is not set — start the server with GOOGLE_MAPS_API_KEY env var");
        }
        log.info("Google Places API key loaded (length={})", apiKey.length());
    }

    public String getNearbyPubs(double lat, double lng, int radius, String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            // "v2:" prefix distinguishes new-API cache entries from legacy-format entries
            String cellKey = String.format("v2:%.2f,%.2f,%d", lat, lng, radius);
            return searchRepo.findValid(cellKey, Instant.now())
                    .map(PlaceSearchCache::getResponseJson)
                    .orElseGet(() -> {
                        String json = callTextSearch(lat, lng, radius, null);
                        // Only cache complete result sets — if there's a nextPageToken the
                        // token would expire long before the 24-hour cache TTL
                        if (isSuccess(json) && !json.contains("\"nextPageToken\"")) {
                            PlaceSearchCache entry = new PlaceSearchCache();
                            entry.setCellKey(cellKey);
                            entry.setResponseJson(json);
                            entry.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
                            searchRepo.save(entry);
                        }
                        return json;
                    });
        }
        return callTextSearch(lat, lng, radius, pageToken);
    }

    public String getPlaceDetails(String placeId, String sessionToken) {
        // "v2:" prefix isolates new-API cached responses from legacy ones
        String cacheKey = "v2:" + placeId;
        return detailsRepo.findValid(cacheKey, Instant.now())
                .map(PlaceDetailsCache::getResponseJson)
                .orElseGet(() -> {
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

    public String getAutocomplete(String input, String sessionToken) {
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
}
