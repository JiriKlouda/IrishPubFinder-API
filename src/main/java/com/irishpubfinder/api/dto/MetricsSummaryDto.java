package com.irishpubfinder.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MetricsSummaryDto {

    private final PeriodCounts today;
    private final PeriodCounts thisWeek;
    private final PeriodCounts thisMonth;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PeriodCounts {
        private final long nearbySearchGoogle;
        private final long nearbySearchCache;
        private final long placeDetailsGoogle;
        private final long placeDetailsCache;
        private final long photoGoogle;
        private final long photoR2;
        private final long autocomplete;
    }
}
