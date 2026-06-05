package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.PlaceDetailsCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PlaceDetailsCacheRepository extends JpaRepository<PlaceDetailsCache, Long> {

    @Query("SELECT c FROM PlaceDetailsCache c WHERE c.placeId = :placeId AND c.expiresAt > :now")
    Optional<PlaceDetailsCache> findValid(@Param("placeId") String placeId, @Param("now") Instant now);

    Optional<PlaceDetailsCache> findByPlaceId(String placeId);
}
