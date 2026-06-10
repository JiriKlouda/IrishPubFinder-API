package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    String email,
    String phoneNumber,
    @NotBlank String password
) {}
