package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.UserUsageDetailDto;
import com.irishpubfinder.api.dto.UserUsageDto;
import com.irishpubfinder.api.service.UsageReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Restricted to ROLE_ADMIN by SecurityConfig (/api/admin/**).
@RestController
@RequestMapping("/api/admin/usage")
@RequiredArgsConstructor
public class AdminUsageController {

    private final UsageReportService usageReportService;

    @GetMapping("/users")
    public ResponseEntity<List<UserUsageDto>> users(
        @RequestParam(defaultValue = "all") String period,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(defaultValue = "30") int limit
    ) {
        return ResponseEntity.ok(usageReportService.listUsers(period, q, offset, limit));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserUsageDetailDto> userDetail(
        @PathVariable String id,
        @RequestParam(defaultValue = "all") String period
    ) {
        return ResponseEntity.ok(usageReportService.userDetail(period, id));
    }
}
