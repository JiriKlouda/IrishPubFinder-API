package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.UserUsageDetailDto;
import com.irishpubfinder.api.dto.UserUsageDto;
import com.irishpubfinder.api.exception.UserNotFoundException;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.repository.ApiCallLogRepository;
import com.irishpubfinder.api.repository.FavouriteRepository;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.UserRepository;
import com.irishpubfinder.api.repository.UserUsageRow;
import com.irishpubfinder.api.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsageReportService {

    // Approximate Google Places API rates (USD per call); mirrors the dashboard's RATES.
    private static final double RATE_NEARBY       = 0.022;
    private static final double RATE_DETAILS      = 0.017;
    private static final double RATE_AUTOCOMPLETE = 0.003;

    private final ApiCallLogRepository logRepo;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final FavouriteRepository favouriteRepository;
    private final FriendshipRepository friendshipRepository;

    public List<UserUsageDto> listUsers(String period, String query, int offset, int limit) {
        Instant from = periodStart(period);
        String q = (query == null || query.isBlank()) ? null : "%" + query.trim().toLowerCase() + "%";
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);

        return logRepo.findTopUsersByCost(from, q, safeLimit, safeOffset).stream()
            .map(UsageReportService::toDto)
            .toList();
    }

    public UserUsageDetailDto userDetail(String period, String userId) {
        Instant from = periodStart(period);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        long nearbyGoogle  = count(userId, ApiMetricsService.NEARBY_SEARCH, from);
        long nearbyCache   = count(userId, ApiMetricsService.NEARBY_SEARCH_CACHE, from);
        long detailsGoogle = count(userId, ApiMetricsService.PLACE_DETAILS, from);
        long detailsCache  = count(userId, ApiMetricsService.PLACE_DETAILS_CACHE, from);
        long autocomplete  = count(userId, ApiMetricsService.AUTOCOMPLETE, from);
        double cost = nearbyGoogle * RATE_NEARBY + detailsGoogle * RATE_DETAILS + autocomplete * RATE_AUTOCOMPLETE;

        return new UserUsageDetailDto(
            user.getId(), user.getEmail(), user.getPhoneNumber(), user.getDisplayName(), user.roleOrDefault().name(),
            nearbyGoogle, nearbyCache, detailsGoogle, detailsCache, autocomplete, cost,
            logRepo.findLastActive(userId),
            visitRepository.countByUserId(userId),
            favouriteRepository.countByUserId(userId),
            friendshipRepository.findAcceptedFriendships(userId).size()
        );
    }

    private long count(String userId, String callType, Instant from) {
        return logRepo.countByUserIdAndCallTypeAndCalledAtGreaterThanEqual(userId, callType, from);
    }

    private static UserUsageDto toDto(UserUsageRow r) {
        long totalCalls = r.getNearbyGoogle() + r.getNearbyCache() + r.getDetailsGoogle()
            + r.getDetailsCache() + r.getAutocomplete();
        return new UserUsageDto(
            r.getUserId(), r.getEmail(), r.getPhoneNumber(), r.getDisplayName(), r.getRole(),
            totalCalls, r.getCost());
    }

    /** Start instant for the named period; "all" → epoch (everything). */
    private static Instant periodStart(String period) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return switch (period == null ? "all" : period.toLowerCase()) {
            case "today" -> now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            case "week"  -> now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1)
                               .atStartOfDay(ZoneOffset.UTC).toInstant();
            case "month" -> now.toLocalDate().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            default      -> Instant.EPOCH;
        };
    }
}
