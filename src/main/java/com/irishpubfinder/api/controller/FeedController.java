package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.FeedItemDto;
import com.irishpubfinder.api.service.FeedService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public List<FeedItemDto> getFeed(@AuthenticationPrincipal String userId) {
        return feedService.getFeed(userId);
    }
}
