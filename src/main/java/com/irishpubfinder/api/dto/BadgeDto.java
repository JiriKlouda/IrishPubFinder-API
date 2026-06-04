package com.irishpubfinder.api.dto;

public record BadgeDto(
    String id,
    String name,
    String description,
    String icon,
    String color,
    String category,
    boolean earned,
    int progress,
    int target
) {}
