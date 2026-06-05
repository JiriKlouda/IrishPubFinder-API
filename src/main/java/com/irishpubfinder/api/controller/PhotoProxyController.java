package com.irishpubfinder.api.controller;

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

    // Only URL-safe characters — blocks path traversal via the R2 object key
    private static final Pattern SAFE_REF = Pattern.compile("^[A-Za-z0-9_\\-]{1,500}$");

    private final GooglePlacesService placesService;
    private final R2StorageService r2;

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

        if (r2.exists(ref)) {
            log.debug("Photo cache HIT: {}", ref);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(r2.publicUrl(ref)))
                    .build();
        }

        log.info("Photo cache MISS — fetching from Google: {}", ref);
        Object[] result = placesService.getPhotoBytes(ref, maxwidth);
        r2.upload(ref, (byte[]) result[0], (String) result[1]);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(r2.publicUrl(ref)))
                .build();
    }
}
