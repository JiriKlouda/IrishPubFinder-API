package com.irishpubfinder.api.dto;

import java.util.List;

public record LeaderboardEntry(
    String userId,
    String email,
    String displayName,
    boolean isCurrentUser,
    int visitCount,
    int badgesEarned,
    List<CoordDto> visitCoordinates
) {}
