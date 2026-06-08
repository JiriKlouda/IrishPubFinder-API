package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.VisitGeoRequest;
import com.irishpubfinder.api.dto.VisitRequest;
import com.irishpubfinder.api.model.Visit;
import com.irishpubfinder.api.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService service;

    @GetMapping
    public ResponseEntity<List<Visit>> getVisits(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(service.getVisits(userId));
    }

    @PostMapping
    public ResponseEntity<Visit> addVisit(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody VisitRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addVisit(userId, request));
    }

    @PutMapping("/geo")
    public ResponseEntity<Void> enrichGeo(
        @AuthenticationPrincipal String userId,
        @RequestBody List<VisitGeoRequest> items
    ) {
        service.enrichGeo(userId, items);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> removeVisit(
        @AuthenticationPrincipal String userId,
        @PathVariable String placeId
    ) {
        service.removeVisit(userId, placeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<Map<String, Boolean>> checkVisit(
        @AuthenticationPrincipal String userId,
        @PathVariable String placeId
    ) {
        return ResponseEntity.ok(Map.of("isVisited", service.isVisited(userId, placeId)));
    }
}
