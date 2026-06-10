package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.Email;

public record FriendSendRequest(
    @Email String email,
    String phoneNumber,
    String userId
) {}
