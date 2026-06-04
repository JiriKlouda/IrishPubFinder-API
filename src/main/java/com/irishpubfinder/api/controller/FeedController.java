package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.FeedItemDto;
import com.irishpubfinder.api.service.FeedService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public List<FeedItemDto> getFeed(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String before
    ) {
        // Default cursor is slightly in the future so the first page includes items created right now
        LocalDateTime cursor = before != null
                ? LocalDateTime.parse(before)
                : LocalDateTime.now().plusSeconds(5);
        return feedService.getFeed(userId, cursor, size);
    }
}
