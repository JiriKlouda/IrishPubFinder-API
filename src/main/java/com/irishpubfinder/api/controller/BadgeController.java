package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.BadgeDto;
import com.irishpubfinder.api.service.BadgeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @GetMapping
    public List<BadgeDto> getBadges(@AuthenticationPrincipal String userId) {
        return badgeService.getBadges(userId);
    }
}
