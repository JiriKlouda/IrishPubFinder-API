package com.irishpubfinder.api.dto;

public record UserMeResponse(
    String userId,
    String email,
    String phoneNumber,
    String displayName,
    String role
) {}
