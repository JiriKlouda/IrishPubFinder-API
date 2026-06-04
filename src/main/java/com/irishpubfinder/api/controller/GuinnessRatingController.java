package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.GuinnessRatingResponse;
import com.irishpubfinder.api.dto.GuinnessReviewRequest;
import com.irishpubfinder.api.service.GuinnessRatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guinness")
public class GuinnessRatingController {

    private final GuinnessRatingService guinnessRatingService;

    public GuinnessRatingController(GuinnessRatingService guinnessRatingService) {
        this.guinnessRatingService = guinnessRatingService;
    }

    @PostMapping("/review")
    public ResponseEntity<Void> submitReview(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody GuinnessReviewRequest request) {
        guinnessRatingService.submitReview(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rating/{placeId}")
    public ResponseEntity<GuinnessRatingResponse> getRating(@PathVariable String placeId) {
        return guinnessRatingService.getRating(placeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
