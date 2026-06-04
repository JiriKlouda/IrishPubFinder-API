package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndPlaceId(String userId, String placeId);
    void deleteByUserIdAndPlaceId(String userId, String placeId);
    long countByUserId(String userId);

    @Query("SELECT COUNT(DISTINCT v.countryCode) FROM Visit v WHERE v.userId = :userId AND v.countryCode IS NOT NULL")
    long countDistinctCountriesByUserId(@Param("userId") String userId);
}
