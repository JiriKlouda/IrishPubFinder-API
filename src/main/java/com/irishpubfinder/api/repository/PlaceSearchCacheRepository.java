package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.PlaceSearchCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PlaceSearchCacheRepository extends JpaRepository<PlaceSearchCache, Long> {

    @Query("SELECT c FROM PlaceSearchCache c WHERE c.cellKey = :key AND c.expiresAt > :now")
    Optional<PlaceSearchCache> findValid(@Param("key") String key, @Param("now") Instant now);

    Optional<PlaceSearchCache> findByCellKey(String cellKey);
}
