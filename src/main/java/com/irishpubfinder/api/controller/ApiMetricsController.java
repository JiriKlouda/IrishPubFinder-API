package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.MetricsSummaryDto;
import com.irishpubfinder.api.service.ApiMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class ApiMetricsController {

    private final ApiMetricsService metricsService;

    @GetMapping
    public ResponseEntity<MetricsSummaryDto> getSummary() {
        return ResponseEntity.ok(metricsService.getSummary());
    }
}
