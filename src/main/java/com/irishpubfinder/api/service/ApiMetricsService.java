package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.MetricsSummaryDto;
import com.irishpubfinder.api.model.ApiCallLog;
import com.irishpubfinder.api.repository.ApiCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiMetricsService {

    public static final String NEARBY_SEARCH         = "NEARBY_SEARCH";
    public static final String NEARBY_SEARCH_CACHE   = "NEARBY_SEARCH_CACHE";
    public static final String PLACE_DETAILS         = "PLACE_DETAILS";
    public static final String PLACE_DETAILS_CACHE   = "PLACE_DETAILS_CACHE";
    public static final String AUTOCOMPLETE          = "AUTOCOMPLETE";
    public static final String PHOTO_GOOGLE          = "PHOTO_GOOGLE";
    public static final String PHOTO_R2              = "PHOTO_R2";

    private final ApiCallLogRepository repo;

    /** Records one API call event. Never throws — metrics must not break request handling. */
    public void record(String callType) {
        try {
            repo.save(ApiCallLog.builder().callType(callType).userId(currentUserId()).build());
        } catch (Exception e) {
            log.warn("Failed to record API metric (type={}): {}", callType, e.getMessage());
        }
    }

    /** The authenticated user on the current request thread, or null (e.g. the public photo endpoint). */
    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof String s && !"anonymousUser".equals(s)) ? s : null;
    }

    public MetricsSummaryDto getSummary() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        Instant startOfDay   = now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfWeek  = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1)
                                  .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfMonth = now.toLocalDate().withDayOfMonth(1)
                                  .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant future = now.plusYears(1).toInstant();

        return MetricsSummaryDto.builder()
                .today(counts(startOfDay, future))
                .thisWeek(counts(startOfWeek, future))
                .thisMonth(counts(startOfMonth, future))
                .build();
    }

    private MetricsSummaryDto.PeriodCounts counts(Instant from, Instant to) {
        return MetricsSummaryDto.PeriodCounts.builder()
                .nearbySearchGoogle(repo.countByCallTypeAndCalledAtBetween(NEARBY_SEARCH, from, to))
                .nearbySearchCache(repo.countByCallTypeAndCalledAtBetween(NEARBY_SEARCH_CACHE, from, to))
                .placeDetailsGoogle(repo.countByCallTypeAndCalledAtBetween(PLACE_DETAILS, from, to))
                .placeDetailsCache(repo.countByCallTypeAndCalledAtBetween(PLACE_DETAILS_CACHE, from, to))
                .photoGoogle(repo.countByCallTypeAndCalledAtBetween(PHOTO_GOOGLE, from, to))
                .photoR2(repo.countByCallTypeAndCalledAtBetween(PHOTO_R2, from, to))
                .autocomplete(repo.countByCallTypeAndCalledAtBetween(AUTOCOMPLETE, from, to))
                .build();
    }
}
