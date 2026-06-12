package com.irishpubfinder.api.dto;

import java.time.LocalDateTime;

public record AdminUserDto(
    String id,
    String email,
    String phoneNumber,
    String displayName,
    String role,
    LocalDateTime createdAt
) {}
