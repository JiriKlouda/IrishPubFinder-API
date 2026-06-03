package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.AuthResponse;
import com.irishpubfinder.api.dto.LoginRequest;
import com.irishpubfinder.api.dto.RegisterRequest;
import com.irishpubfinder.api.exception.EmailAlreadyExistsException;
import com.irishpubfinder.api.exception.InvalidCredentialsException;
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
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }
        User user = User.builder()
            .id(UUID.randomUUID().toString())
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
            .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }
}
