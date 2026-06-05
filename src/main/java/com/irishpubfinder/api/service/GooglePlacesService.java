package com.irishpubfinder.api.service;

import com.irishpubfinder.api.exception.PlacesApiException;
import com.irishpubfinder.api.model.PlaceDetailsCache;
import com.irishpubfinder.api.model.PlaceSearchCache;
import com.irishpubfinder.api.repository.PlaceDetailsCacheRepository;
import com.irishpubfinder.api.repository.PlaceSearchCacheRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class GooglePlacesService {

    private static final String GOOGLE_HOST = "maps.googleapis.com";
    private static final String PLACES_BASE_PATH = "/maps/api/place";
    private static final String PLACE_DETAILS_FIELDS =
            "geometry,formatted_address,name,formatted_phone_number,website,opening_hours,rating";

    @Value("${google.places.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
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
            String cellKey = String.format("%.2f,%.2f,%d", lat, lng, radius);
            return searchRepo.findValid(cellKey, Instant.now())
                    .map(PlaceSearchCache::getResponseJson)
                    .orElseGet(() -> {
                        String json = callTextSearch(lat, lng, radius, null);
                        if (isSuccess(json)) {
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
        return detailsRepo.findValid(placeId, Instant.now())
                .map(PlaceDetailsCache::getResponseJson)
                .orElseGet(() -> {
                    String json = callPlaceDetails(placeId, sessionToken);
                    if (isSuccess(json)) {
                        PlaceDetailsCache entry = detailsRepo.findByPlaceId(placeId)
                                .orElse(new PlaceDetailsCache());
                        entry.setPlaceId(placeId);
                        entry.setResponseJson(json);
                        entry.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
                        detailsRepo.save(entry);
                    }
                    return json;
                });
    }

    /**
     * Fetches raw photo bytes from the Google Places Photo API.
     * Returns [byte[], contentType]. RestTemplate follows Google's 302 redirect automatically.
     */
    public Object[] getPhotoBytes(String photoRef, int maxwidth) {
        String query = "photoreference=" + encode(photoRef)
                + "&maxwidth=" + maxwidth
                + "&key=" + apiKey;
        URI uri = buildUri(PLACES_BASE_PATH + "/photo", query);
        log.info("Google Places Photo → fetching (ref length={})", photoRef.length());
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

    /** Only cache responses where Google confirmed success — never cache error/denied responses. */
    private static boolean isSuccess(String json) {
        return json.contains("\"status\":\"OK\"") || json.contains("\"status\":\"ZERO_RESULTS\"");
    }

    public String getAutocomplete(String input, String sessionToken) {
        String query = "input=" + encode(input)
                + "&types=" + encode("(regions)")
                + "&language=en"
                + (sessionToken != null && !sessionToken.isBlank() ? "&sessiontoken=" + encode(sessionToken) : "")
                + "&key=" + apiKey;
        return callGoogle(buildUri(PLACES_BASE_PATH + "/autocomplete/json", query));
    }

    private String callTextSearch(double lat, double lng, int radius, String pageToken) {
        // Google requires pagetoken requests to contain ONLY pagetoken + key.
        // Including query/location/radius alongside a pagetoken causes INVALID_REQUEST.
        String query = (pageToken != null && !pageToken.isBlank())
                ? "pagetoken=" + encode(pageToken) + "&key=" + apiKey
                : "query=" + encode("irish pubs near me")
                        + "&location=" + lat + "," + lng
                        + "&radius=" + radius
                        + "&key=" + apiKey;
        return callGoogle(buildUri(PLACES_BASE_PATH + "/textsearch/json", query));
    }

    private String callPlaceDetails(String placeId, String sessionToken) {
        String query = "place_id=" + encode(placeId)
                + "&fields=" + encode(PLACE_DETAILS_FIELDS)
                + "&language=en"
                + (sessionToken != null && !sessionToken.isBlank() ? "&sessiontoken=" + encode(sessionToken) : "")
                + "&key=" + apiKey;
        return callGoogle(buildUri(PLACES_BASE_PATH + "/details/json", query));
    }

    private String callGoogle(URI uri) {
        log.info("Google Places → {}", uri.toString().replace(apiKey, "<KEY>"));
        try {
            String response = restTemplate.getForObject(uri, String.class);
            String result = response != null ? response : "{}";
            log.info("Google Places ← status snippet: {}",
                    result.substring(0, Math.min(120, result.length())));
            return result;
        } catch (RestClientException ex) {
            throw new PlacesApiException("Google Places API unavailable: " + ex.getMessage());
        }
    }

    private URI buildUri(String path, String encodedQuery) {
        // query is already percent-encoded by our encode() helper — use URI.create() which
        // treats the string as already valid so there is no double-encoding.
        return URI.create("https://" + GOOGLE_HOST + path + "?" + encodedQuery);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
