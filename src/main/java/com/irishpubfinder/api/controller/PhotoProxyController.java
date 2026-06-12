package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.exception.PlacesApiException;
import com.irishpubfinder.api.service.ApiMetricsService;
import com.irishpubfinder.api.service.GooglePlacesService;
import com.irishpubfinder.api.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PhotoProxyController {

    // New Places API photo name format: places/{placeId}/photos/{token}
    private static final Pattern SAFE_REF = Pattern.compile(
            "^places/[A-Za-z0-9_\\-]+/photos/[A-Za-z0-9_\\-]+$");

    private final GooglePlacesService placesService;
    private final R2StorageService r2;
    private final ApiMetricsService metrics;

    @GetMapping("/photo")
    public ResponseEntity<Void> photo(
            @RequestParam("ref") String ref,
            @RequestParam(value = "maxwidth", defaultValue = "400") int maxwidth) {

        if (!SAFE_REF.matcher(ref).matches()) {
            return ResponseEntity.badRequest().build();
        }
        if (!r2.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        maxwidth = Math.min(Math.max(maxwidth, 1), 1600);

        // Key by the stable placeId, not the full photo name. The new Places API regenerates
        // the photo-name token on every search, so keying by it would always miss the cache.
        // ref is "places/{placeId}/photos/{token}" (already validated by SAFE_REF).
        String placeId = ref.split("/")[1];
        String r2Key = "place_" + placeId;

        if (r2.exists(r2Key)) {
            log.debug("Photo cache HIT: {}", r2Key);
            metrics.record(ApiMetricsService.PHOTO_R2);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(r2.publicUrl(r2Key)))
                    .build();
        }

        log.info("Photo cache MISS — fetching from Google: {}", ref);
        Object[] result = fetchPhotoBytes(ref, placeId, maxwidth);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        r2.upload(r2Key, (byte[]) result[0], (String) result[1]);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(r2.publicUrl(r2Key)))
                .build();
    }

    /**
     * Fetches the photo bytes for a cold cache miss. Tries the supplied ref first; if its token
     * is stale (common for old saved visits, whose stored photo token has since expired), it
     * re-resolves a current photo name for the place via Place Details and retries. Returns null
     * if no photo can be obtained.
     */
    private Object[] fetchPhotoBytes(String ref, String placeId, int maxwidth) {
        metrics.record(ApiMetricsService.PHOTO_GOOGLE);
        try {
            return placesService.getPhotoBytes(ref, maxwidth);
        } catch (PlacesApiException ex) {
            log.info("Stale photo ref for place {} — re-resolving via place details", placeId);
            String freshRef = placesService.getFirstPhotoName(placeId);
            if (freshRef == null) {
                return null;
            }
            try {
                return placesService.getPhotoBytes(freshRef, maxwidth);
            } catch (PlacesApiException retryEx) {
                log.warn("Photo unavailable for place {} after re-resolve: {}", placeId, retryEx.getMessage());
                return null;
            }
        }
    }
}
