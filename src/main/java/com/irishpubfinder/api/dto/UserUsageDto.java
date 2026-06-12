package com.irishpubfinder.api.dto;

/** One row in the per-user API usage list (ranked by cost). */
public record UserUsageDto(
    String userId,
    String email,
    String phoneNumber,
    String displayName,
    String role,
    long totalCalls,
    double cost
) {}
