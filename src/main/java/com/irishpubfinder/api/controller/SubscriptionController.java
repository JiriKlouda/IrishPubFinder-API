package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Called by the mobile app immediately after a successful RevenueCat purchase or restore.
     * Verifies the entitlement via RevenueCat's REST API and upgrades the user's role to PRO
     * if active. The app calls GET /api/users/me afterwards to pick up the updated role.
     */
    @PostMapping("/sync")
    public ResponseEntity<Void> sync(@AuthenticationPrincipal String userId) {
        subscriptionService.syncSubscription(userId);
        return ResponseEntity.noContent().build();
    }
}
