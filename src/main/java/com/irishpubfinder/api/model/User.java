package com.irishpubfinder.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Column(unique = true, nullable = true)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "display_name", length = 50)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Nullable in the DB so ddl-auto=update adds it cleanly to existing rows; read via
    // roleOrDefault() which treats null (legacy rows) as USER.
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (role == null) role = UserRole.USER;
    }

    /** Never returns null — legacy rows with a null role are treated as USER. */
    public UserRole roleOrDefault() {
        return role != null ? role : UserRole.USER;
    }
}
