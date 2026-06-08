package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.VisitGeoRequest;
import com.irishpubfinder.api.dto.VisitRequest;
import com.irishpubfinder.api.exception.VisitNotFoundException;
import com.irishpubfinder.api.model.Visit;
import com.irishpubfinder.api.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository repository;
    private final BadgeService badgeService;

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
            .continent(request.continent())
            .irishCounty(request.irishCounty())
            .usState(request.usState())
            .city(request.city())
            .build();
        Visit saved = repository.save(visit);
        badgeService.checkAndRecordNewBadges(userId);
        return saved;
    }

    /** Backfill geo-enrichment (continent/county/state/city) for existing visits, then re-check badges. */
    @Transactional
    public void enrichGeo(String userId, List<VisitGeoRequest> items) {
        if (items == null || items.isEmpty()) return;
        Map<String, Visit> byPlaceId = repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .collect(Collectors.toMap(Visit::getPlaceId, Function.identity(), (a, b) -> a));
        boolean changed = false;
        for (VisitGeoRequest item : items) {
            Visit v = byPlaceId.get(item.placeId());
            if (v == null) continue;
            boolean updated = false;
            if (item.continent() != null && !item.continent().equals(v.getContinent())) { v.setContinent(item.continent()); updated = true; }
            if (item.irishCounty() != null && !item.irishCounty().equals(v.getIrishCounty())) { v.setIrishCounty(item.irishCounty()); updated = true; }
            if (item.usState() != null && !item.usState().equals(v.getUsState())) { v.setUsState(item.usState()); updated = true; }
            if (item.city() != null && !item.city().equals(v.getCity())) { v.setCity(item.city()); updated = true; }
            if (updated) { repository.save(v); changed = true; }
        }
        if (changed) badgeService.checkAndRecordNewBadges(userId);
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
