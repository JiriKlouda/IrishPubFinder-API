package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateNameRequest(@Size(max = 50) String displayName) {}
