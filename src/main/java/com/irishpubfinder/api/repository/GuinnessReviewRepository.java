package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.GuinnessReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuinnessReviewRepository extends JpaRepository<GuinnessReview, Long> {
    List<GuinnessReview> findByPlaceId(String placeId);
    Optional<GuinnessReview> findByUserIdAndPlaceId(String userId, String placeId);
    long countByUserId(String userId);

    void deleteAllByUserId(String userId);
}
