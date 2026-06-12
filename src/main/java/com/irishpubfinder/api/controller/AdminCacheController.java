package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.repository.PlaceDetailsCacheRepository;
import com.irishpubfinder.api.repository.PlaceSearchCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Restricted to ROLE_ADMIN by SecurityConfig (/api/admin/**). Testing aid only — clears the
// Google Places search/details caches so the next requests are cold fetches. Does NOT touch
// R2 photos (those are cached indefinitely and never deleted here).
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
public class AdminCacheController {

    private final PlaceSearchCacheRepository searchRepo;
    private final PlaceDetailsCacheRepository detailsRepo;

    @PostMapping("/clear")
    @Transactional
    public ResponseEntity<Map<String, Long>> clear() {
        long search = searchRepo.count();
        long details = detailsRepo.count();
        searchRepo.deleteAllInBatch();
        detailsRepo.deleteAllInBatch();
        return ResponseEntity.ok(Map.of(
            "placeSearchCleared", search,
            "placeDetailsCleared", details
        ));
    }
}
