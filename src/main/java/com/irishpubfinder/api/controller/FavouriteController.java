package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.FavouriteRequest;
import com.irishpubfinder.api.model.Favourite;
import com.irishpubfinder.api.service.FavouriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/favourites")
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouriteService service;

    // GET /api/users/{userId}/favourites
    @GetMapping
    public ResponseEntity<List<Favourite>> getFavourites(@PathVariable String userId) {
        return ResponseEntity.ok(service.getFavourites(userId));
    }

    // POST /api/users/{userId}/favourites
    @PostMapping
    public ResponseEntity<Favourite> addFavourite(
        @PathVariable String userId,
        @Valid @RequestBody FavouriteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addFavourite(userId, request));
    }

    // DELETE /api/users/{userId}/favourites/{placeId}
    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> removeFavourite(
        @PathVariable String userId,
        @PathVariable String placeId
    ) {
        service.removeFavourite(userId, placeId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/users/{userId}/favourites/{placeId}
    @GetMapping("/{placeId}")
    public ResponseEntity<Map<String, Boolean>> checkFavourite(
        @PathVariable String userId,
        @PathVariable String placeId
    ) {
        return ResponseEntity.ok(Map.of("isFavourite", service.isFavourite(userId, placeId)));
    }
}
