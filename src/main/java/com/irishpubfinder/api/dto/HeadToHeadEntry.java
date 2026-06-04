package com.irishpubfinder.api.dto;

import java.util.List;

public record HeadToHeadEntry(
    String userId,
    String email,
    String displayName,
    int visitCount,
    int favouriteCount,
    int guinnessCount,
    int badgesEarned,
    List<CoordDto> visitCoordinates
) {}
