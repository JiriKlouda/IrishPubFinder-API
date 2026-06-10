package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.AuthResponse;
import com.irishpubfinder.api.dto.LoginRequest;
import com.irishpubfinder.api.dto.RegisterRequest;
import com.irishpubfinder.api.exception.EmailAlreadyExistsException;
import com.irishpubfinder.api.exception.InvalidCredentialsException;
import com.irishpubfinder.api.exception.PhoneAlreadyExistsException;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.repository.UserRepository;
import com.irishpubfinder.api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalise(request.email());
        String phone = normalisePhone(request.phoneNumber());

        if (email == null && phone == null) {
            throw new IllegalArgumentException("An email address or phone number is required");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }
        if (phone != null && userRepository.existsByPhoneNumber(phone)) {
            throw new PhoneAlreadyExistsException("An account with this phone number already exists");
        }

        String displayName = normalise(request.displayName());
        User user = User.builder()
            .id(UUID.randomUUID().toString())
            .email(email)
            .phoneNumber(phone)
            .passwordHash(passwordEncoder.encode(request.password()))
            .displayName(displayName)
            .build();
        userRepository.save(user);
        return toAuthResponse(jwtUtil.generateToken(user.getId(), user.getEmail(), user.getPhoneNumber()), user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalise(request.email());
        String phone = normalisePhone(request.phoneNumber());

        if (email == null && phone == null) {
            throw new InvalidCredentialsException();
        }

        User user = email != null
            ? userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new)
            : userRepository.findByPhoneNumber(phone).orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(jwtUtil.generateToken(user.getId(), user.getEmail(), user.getPhoneNumber()), user);
    }

    private static AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getPhoneNumber(), user.getDisplayName());
    }

    private static String normalise(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase();
    }

    private static String normalisePhone(String value) {
        if (value == null || value.isBlank()) return null;
        String stripped = value.trim().replaceAll("[\\s\\-()]", "");
        return stripped.isEmpty() ? null : stripped;
    }
}
