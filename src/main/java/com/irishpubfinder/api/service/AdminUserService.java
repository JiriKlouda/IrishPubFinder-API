package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.AdminUserDto;
import com.irishpubfinder.api.exception.UserNotFoundException;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.model.UserRole;
import com.irishpubfinder.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public List<AdminUserDto> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(AdminUserService::toDto)
            .toList();
    }

    @Transactional
    public AdminUserDto updateRole(String actingUserId, String targetUserId, String roleValue) {
        if (actingUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }
        UserRole role = parseRole(roleValue);
        User user = userRepository.findById(targetUserId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));
        user.setRole(role);
        return toDto(userRepository.save(user));
    }

    private static UserRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A role is required.");
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + value);
        }
    }

    private static AdminUserDto toDto(User u) {
        return new AdminUserDto(
            u.getId(), u.getEmail(), u.getPhoneNumber(), u.getDisplayName(),
            u.roleOrDefault().name(), u.getCreatedAt());
    }
}
