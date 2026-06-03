package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.LeaderboardEntry;
import com.irishpubfinder.api.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService service;

    @GetMapping
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(service.getLeaderboard(userId));
    }
}
