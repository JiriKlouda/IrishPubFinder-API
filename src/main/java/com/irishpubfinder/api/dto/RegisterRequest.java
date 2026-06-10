package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email String email,
    String phoneNumber,
    @Size(min = 8, message = "Password must be at least 8 characters") String password,
    @Size(max = 50) String displayName
) {}
