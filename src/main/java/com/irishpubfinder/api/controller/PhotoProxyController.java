package com.irishpubfinder.api.controller;

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
        metrics.record(ApiMetricsService.PHOTO_GOOGLE);
        Object[] result = placesService.getPhotoBytes(ref, maxwidth);
        r2.upload(r2Key, (byte[]) result[0], (String) result[1]);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(r2.publicUrl(r2Key)))
                .build();
    }
}
