package com.irishpubfinder.api.dto;

public record PendingRequestResponse(
    String friendshipId,
    String fromUserId,
    String fromEmail,
    String fromDisplayName,
    String sentAt
) {}
