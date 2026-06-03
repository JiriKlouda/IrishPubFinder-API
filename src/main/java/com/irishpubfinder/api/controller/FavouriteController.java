package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.FavouriteRequest;
import com.irishpubfinder.api.model.Favourite;
import com.irishpubfinder.api.service.FavouriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favourites")
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouriteService service;

    @GetMapping
    public ResponseEntity<List<Favourite>> getFavourites(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(service.getFavourites(userId));
    }

    @PostMapping
    public ResponseEntity<Favourite> addFavourite(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody FavouriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addFavourite(userId, request));
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> removeFavourite(
        @AuthenticationPrincipal String userId,
        @PathVariable String placeId
    ) {
        service.removeFavourite(userId, placeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<Map<String, Boolean>> checkFavourite(
        @AuthenticationPrincipal String userId,
        @PathVariable String placeId
    ) {
        return ResponseEntity.ok(Map.of("isFavourite", service.isFavourite(userId, placeId)));
    }
}
