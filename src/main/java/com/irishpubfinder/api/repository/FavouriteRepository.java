package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.Favourite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavouriteRepository extends JpaRepository<Favourite, Long> {

    List<Favourite> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Favourite> findByUserIdAndPlaceId(String userId, String placeId);

    boolean existsByUserIdAndPlaceId(String userId, String placeId);

    void deleteByUserIdAndPlaceId(String userId, String placeId);

    long countByUserId(String userId);
}
