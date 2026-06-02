package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavouriteRequest(
    @NotBlank String placeId,
    @NotBlank String name,
    @NotBlank String address,
    @NotNull Double latitude,
    @NotNull Double longitude,
    Double rating,
    String mapsUrl,
    String photoUrl
) {}
