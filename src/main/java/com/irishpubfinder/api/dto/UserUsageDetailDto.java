package com.irishpubfinder.api.dto;

import java.time.Instant;

/** Full per-user usage breakdown for the detail screen, over a selected period. */
public record UserUsageDetailDto(
    String userId,
    String email,
    String phoneNumber,
    String displayName,
    String role,

    // API usage (attributable calls only)
    long nearbyGoogle,
    long nearbyCache,
    long detailsGoogle,
    long detailsCache,
    long autocomplete,
    double cost,

    Instant lastActive,

    // Engagement
    long visits,
    long favourites,
    long friends
) {}
