package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.Email;

public record UpdateEmailRequest(@Email String email) {}
