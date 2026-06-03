package com.irishpubfinder.api.dto;

public record FriendResponse(
    String friendshipId,
    String userId,
    String email,
    String displayName,
    String connectedAt
) {}
