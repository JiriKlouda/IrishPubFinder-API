package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.GuinnessRatingResponse;
import com.irishpubfinder.api.dto.GuinnessReviewRequest;
import com.irishpubfinder.api.model.GuinnessReview;
import com.irishpubfinder.api.repository.GuinnessReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GuinnessRatingService {

    private final GuinnessReviewRepository reviewRepository;

    public GuinnessRatingService(GuinnessReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public GuinnessReview submitReview(String userId, GuinnessReviewRequest request) {
        GuinnessReview review = reviewRepository
                .findByUserIdAndPlaceId(userId, request.getPlaceId())
                .orElse(GuinnessReview.builder()
                        .userId(userId)
                        .placeId(request.getPlaceId())
                        .build());

        review.setCreaminess(request.getCreaminess());
        review.setTemperature(request.getTemperature());
        review.setQuality(request.getQuality());
        review.setPrice(request.getPrice());
        review.setOverall(request.getOverall());

        return reviewRepository.save(review);
    }

    public Optional<GuinnessRatingResponse> getRating(String placeId) {
        List<GuinnessReview> reviews = reviewRepository.findByPlaceId(placeId);
        if (reviews.isEmpty()) return Optional.empty();

        double avgCreaminess  = reviews.stream().mapToInt(GuinnessReview::getCreaminess).average().orElse(0);
        double avgTemperature = reviews.stream().mapToInt(GuinnessReview::getTemperature).average().orElse(0);
        double avgQuality     = reviews.stream().mapToInt(GuinnessReview::getQuality).average().orElse(0);
        double avgPrice       = reviews.stream().mapToInt(GuinnessReview::getPrice).average().orElse(0);
        double avgOverall     = reviews.stream().mapToInt(GuinnessReview::getOverall).average().orElse(0);

        // Weighted: quality 35%, creaminess 25%, overall 20%, temperature 10%, price 10%
        double overallScore = (avgQuality * 0.35) + (avgCreaminess * 0.25)
                + (avgOverall * 0.20) + (avgTemperature * 0.10) + (avgPrice * 0.10);

        return Optional.of(new GuinnessRatingResponse(
                placeId,
                round(avgCreaminess),
                round(avgTemperature),
                round(avgQuality),
                round(avgPrice),
                round(avgOverall),
                round(overallScore),
                reviews.size()
        ));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
