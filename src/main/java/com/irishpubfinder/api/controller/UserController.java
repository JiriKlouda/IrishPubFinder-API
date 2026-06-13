package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.UpdateEmailRequest;
import com.irishpubfinder.api.dto.UpdateNameRequest;
import com.irishpubfinder.api.dto.UpdatePhoneRequest;
import com.irishpubfinder.api.dto.UserMeResponse;
import com.irishpubfinder.api.exception.EmailAlreadyExistsException;
import com.irishpubfinder.api.exception.PhoneAlreadyExistsException;
import com.irishpubfinder.api.repository.UserRepository;
import com.irishpubfinder.api.service.AccountDeletionService;
import com.irishpubfinder.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AccountDeletionService accountDeletionService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal String userId) {
        return userRepository.findById(userId)
            // Apply the admin allowlist here too, so an allowlisted account is promoted on
            // the next app launch (refreshMe) without needing to sign out and back in.
            .map(authService::ensureAdminAllowlist)
            .<ResponseEntity<UserMeResponse>>map(u -> ResponseEntity.ok(new UserMeResponse(
                u.getId(), u.getEmail(), u.getPhoneNumber(), u.getDisplayName(), u.roleOrDefault().name())))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateName(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody UpdateNameRequest request
    ) {
        userRepository.findById(userId).ifPresent(user -> {
            String name = (request.displayName() != null && !request.displayName().isBlank())
                ? request.displayName().trim()
                : null;
            user.setDisplayName(name);
            userRepository.save(user);
        });
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/phone")
    public ResponseEntity<Void> updatePhone(
        @AuthenticationPrincipal String userId,
        @RequestBody UpdatePhoneRequest request
    ) {
        String phone = request.phoneNumber() == null || request.phoneNumber().isBlank()
            ? null
            : request.phoneNumber().trim().replaceAll("[\\s\\-()]", "");

        if (phone != null && userRepository.existsByPhoneNumber(phone)) {
            throw new PhoneAlreadyExistsException("An account with this phone number already exists");
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setPhoneNumber(phone);
            userRepository.save(user);
        });
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal String userId) {
        accountDeletionService.deleteAccount(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/me/email")
    public ResponseEntity<Void> updateEmail(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody UpdateEmailRequest request
    ) {
        String email = request.email() == null || request.email().isBlank()
            ? null
            : request.email().trim().toLowerCase();

        if (email != null && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setEmail(email);
            userRepository.save(user);
        });
        return ResponseEntity.noContent().build();
    }
}
