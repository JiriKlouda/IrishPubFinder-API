package com.irishpubfinder.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.model.UserRole;
import com.irishpubfinder.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies RevenueCat entitlements for a user and updates their role accordingly.
 *
 * RevenueCat identifies subscribers by appUserID, which we set to the user's UUID at
 * SDK initialisation time on the client. The backend calls RevenueCat's REST API using
 * the same ID to retrieve live entitlement status, then promotes or demotes the role.
 *
 * Setup:
 *  1. Set revenuecat.secret-key in application.properties (or env var REVENUECAT_SECRET_KEY).
 *  2. Configure an entitlement named "pro" in the RevenueCat dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String RC_API_BASE = "https://api.revenuecat.com/v1";
    private static final String PRO_ENTITLEMENT = "pro";

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${revenuecat.secret-key:}")
    private String revenueCatSecretKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Fetches the user's entitlements from RevenueCat and sets their role to PRO when
     * the "pro" entitlement is active, or back to USER when it is not.
     *
     * @param userId the authenticated user's UUID (used as RevenueCat appUserID)
     */
    @Transactional
    public void syncSubscription(String userId) {
        if (revenueCatSecretKey == null || revenueCatSecretKey.isBlank()) {
            log.warn("revenuecat.secret-key is not configured — subscription sync skipped");
            return;
        }

        boolean hasPro = fetchProEntitlement(userId);

        userRepository.findById(userId).ifPresent(user -> {
            UserRole currentRole = user.roleOrDefault();
            // Only modify USER <-> PRO transitions; leave ADMIN and FREE_PRO untouched.
            if (hasPro && currentRole == UserRole.USER) {
                user.setRole(UserRole.PRO);
                userRepository.save(user);
                log.info("User {} upgraded to PRO via RevenueCat", userId);
            } else if (!hasPro && currentRole == UserRole.PRO) {
                user.setRole(UserRole.USER);
                userRepository.save(user);
                log.info("User {} downgraded to USER (RevenueCat entitlement inactive)", userId);
            }
        });
    }

    private boolean fetchProEntitlement(String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RC_API_BASE + "/subscribers/" + userId))
                .header("Authorization", "Bearer " + revenueCatSecretKey)
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("RevenueCat API returned {} for user {}", response.statusCode(), userId);
                return false;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode entitlements = root.path("subscriber").path("entitlements");
            JsonNode proNode = entitlements.path(PRO_ENTITLEMENT);

            if (proNode.isMissingNode()) return false;

            String expiresDate = proNode.path("expires_date").asText(null);
            // expires_date is null for lifetime entitlements; non-null means it's time-limited
            // but still active at the time RevenueCat returns it in the active set.
            return !proNode.isMissingNode() && (expiresDate == null || isNotExpired(expiresDate));
        } catch (Exception e) {
            log.error("Failed to fetch RevenueCat entitlements for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    private boolean isNotExpired(String isoDate) {
        try {
            return java.time.Instant.parse(isoDate).isAfter(java.time.Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
}
