package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.ApiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {

    long countByCallTypeAndCalledAtBetween(String callType, Instant from, Instant to);

    // Per-user, per-type count for the detail view (from-boundary inclusive).
    long countByUserIdAndCallTypeAndCalledAtGreaterThanEqual(String userId, String callType, Instant from);

    @Query("SELECT MAX(l.calledAt) FROM ApiCallLog l WHERE l.userId = :userId")
    Instant findLastActive(@Param("userId") String userId);

    /**
     * Users ranked by attributable Google API cost over a period, paginated and optionally
     * searched by name/email/phone. Cost weights: nearby $0.022, details $0.017, autocomplete
     * $0.003 (cache hits are free). {@code :q} is a lowercased %like% pattern or null.
     */
    @Query(value = """
        SELECT u.id AS userId,
               u.email AS email,
               u.display_name AS displayName,
               u.phone_number AS phoneNumber,
               u.role AS role,
               SUM(CASE WHEN l.call_type = 'NEARBY_SEARCH' THEN 1 ELSE 0 END) AS nearbyGoogle,
               SUM(CASE WHEN l.call_type = 'NEARBY_SEARCH_CACHE' THEN 1 ELSE 0 END) AS nearbyCache,
               SUM(CASE WHEN l.call_type = 'PLACE_DETAILS' THEN 1 ELSE 0 END) AS detailsGoogle,
               SUM(CASE WHEN l.call_type = 'PLACE_DETAILS_CACHE' THEN 1 ELSE 0 END) AS detailsCache,
               SUM(CASE WHEN l.call_type = 'AUTOCOMPLETE' THEN 1 ELSE 0 END) AS autocomplete,
               (SUM(CASE WHEN l.call_type = 'NEARBY_SEARCH' THEN 1 ELSE 0 END) * 0.022
                + SUM(CASE WHEN l.call_type = 'PLACE_DETAILS' THEN 1 ELSE 0 END) * 0.017
                + SUM(CASE WHEN l.call_type = 'AUTOCOMPLETE' THEN 1 ELSE 0 END) * 0.003) AS cost
        FROM api_call_logs l
        JOIN users u ON u.id = l.user_id
        WHERE l.called_at >= :from
          AND (CAST(:q AS text) IS NULL
               OR LOWER(u.email) LIKE :q
               OR LOWER(u.display_name) LIKE :q
               OR u.phone_number LIKE :q)
        GROUP BY u.id, u.email, u.display_name, u.phone_number, u.role
        ORDER BY cost DESC, u.id
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<UserUsageRow> findTopUsersByCost(@Param("from") Instant from,
                                          @Param("q") String q,
                                          @Param("limit") int limit,
                                          @Param("offset") int offset);
}
