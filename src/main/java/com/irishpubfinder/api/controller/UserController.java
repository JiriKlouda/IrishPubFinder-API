package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.UpdateNameRequest;
import com.irishpubfinder.api.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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
}
