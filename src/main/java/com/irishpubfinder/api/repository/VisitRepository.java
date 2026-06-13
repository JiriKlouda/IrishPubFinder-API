package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.Visit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByUserIdAndPlaceId(String userId, String placeId);
    void deleteByUserIdAndPlaceId(String userId, String placeId);
    long countByUserId(String userId);

    @Query("SELECT COUNT(DISTINCT v.countryCode) FROM Visit v WHERE v.userId = :userId AND v.countryCode IS NOT NULL")
    long countDistinctCountriesByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT v.countryCode FROM Visit v WHERE v.userId = :userId AND v.countryCode IS NOT NULL")
    List<String> findDistinctCountryCodesByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(DISTINCT v.continent) FROM Visit v WHERE v.userId = :userId AND v.continent IS NOT NULL")
    long countDistinctContinentsByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(DISTINCT v.irishCounty) FROM Visit v WHERE v.userId = :userId AND v.irishCounty IS NOT NULL")
    long countDistinctIrishCountiesByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(DISTINCT v.usState) FROM Visit v WHERE v.userId = :userId AND v.usState IS NOT NULL")
    long countDistinctUsStatesByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(DISTINCT v.city) FROM Visit v WHERE v.userId = :userId AND v.city IS NOT NULL")
    long countDistinctCitiesByUserId(@Param("userId") String userId);

    void deleteAllByUserId(String userId);

    @Query("SELECT v FROM Visit v WHERE v.userId IN :userIds AND v.createdAt < :before ORDER BY v.createdAt DESC")
    List<Visit> findPageByUserIds(@Param("userIds") List<String> userIds,
                                  @Param("before") LocalDateTime before,
                                  Pageable pageable);
}
