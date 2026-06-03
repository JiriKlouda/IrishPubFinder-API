package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FriendSendRequest(
    @NotBlank @Email String email
) {}
