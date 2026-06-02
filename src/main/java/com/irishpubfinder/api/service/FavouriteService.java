package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.FavouriteRequest;
import com.irishpubfinder.api.exception.DuplicateFavouriteException;
import com.irishpubfinder.api.exception.FavouriteNotFoundException;
import com.irishpubfinder.api.model.Favourite;
import com.irishpubfinder.api.repository.FavouriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavouriteService {

    private final FavouriteRepository repository;

    public List<Favourite> getFavourites(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Favourite addFavourite(String userId, FavouriteRequest request) {
        if (repository.existsByUserIdAndPlaceId(userId, request.placeId())) {
            throw new DuplicateFavouriteException("Pub already in favourites");
        }
        Favourite favourite = Favourite.builder()
            .userId(userId)
            .placeId(request.placeId())
            .name(request.name())
            .address(request.address())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .rating(request.rating())
            .mapsUrl(request.mapsUrl())
            .photoUrl(request.photoUrl())
            .build();
        return repository.save(favourite);
    }

    @Transactional
    public void removeFavourite(String userId, String placeId) {
        if (!repository.existsByUserIdAndPlaceId(userId, placeId)) {
            throw new FavouriteNotFoundException("Favourite not found for placeId: " + placeId);
        }
        repository.deleteByUserIdAndPlaceId(userId, placeId);
    }

    public boolean isFavourite(String userId, String placeId) {
        return repository.existsByUserIdAndPlaceId(userId, placeId);
    }
}
