package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Geo-enrichment for an existing visit, used to backfill county/state/city/continent. */
public record VisitGeoRequest(
    @NotBlank String placeId,
    String continent,
    String irishCounty,
    String usState,
    String city
) {}
