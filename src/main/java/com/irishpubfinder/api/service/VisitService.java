package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.VisitRequest;
import com.irishpubfinder.api.exception.VisitNotFoundException;
import com.irishpubfinder.api.model.Visit;
import com.irishpubfinder.api.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository repository;

    public List<Visit> getVisits(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Visit addVisit(String userId, VisitRequest request) {
        if (repository.existsByUserIdAndPlaceId(userId, request.placeId())) {
            return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(v -> v.getPlaceId().equals(request.placeId()))
                .findFirst()
                .orElseThrow();
        }
        Visit visit = Visit.builder()
            .userId(userId)
            .placeId(request.placeId())
            .name(request.name())
            .address(request.address())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .rating(request.rating())
            .mapsUrl(request.mapsUrl())
            .photoUrl(request.photoUrl())
            .countryCode(request.countryCode())
            .build();
        return repository.save(visit);
    }

    @Transactional
    public void removeVisit(String userId, String placeId) {
        if (!repository.existsByUserIdAndPlaceId(userId, placeId)) {
            throw new VisitNotFoundException("Visit not found for placeId: " + placeId);
        }
        repository.deleteByUserIdAndPlaceId(userId, placeId);
    }

    public boolean isVisited(String userId, String placeId) {
        return repository.existsByUserIdAndPlaceId(userId, placeId);
    }
}
