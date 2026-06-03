package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndPlaceId(String userId, String placeId);
    void deleteByUserIdAndPlaceId(String userId, String placeId);
}
