package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Geo-enrichment for an existing visit, used to backfill country/county/state/city/continent. */
public record VisitGeoRequest(
    @NotBlank String placeId,
    String countryCode,
    String continent,
    String irishCounty,
    String usState,
    String city
) {}
