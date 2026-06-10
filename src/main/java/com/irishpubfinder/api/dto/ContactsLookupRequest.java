package com.irishpubfinder.api.dto;

import java.util.List;

public record ContactsLookupRequest(List<String> phoneNumbers) {}
