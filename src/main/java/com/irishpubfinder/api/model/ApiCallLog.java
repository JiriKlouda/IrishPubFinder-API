package com.irishpubfinder.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "api_call_logs", indexes = {
        @Index(name = "idx_api_call_logs_called_at", columnList = "called_at"),
        @Index(name = "idx_api_call_logs_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "call_type", nullable = false, length = 40)
    private String callType;

    // The authenticated user who triggered the call; null for unauthenticated calls
    // (e.g. the public photo endpoint, which the native image loader hits with no token).
    @Column(name = "user_id")
    private String userId;

    @Column(name = "called_at", nullable = false, updatable = false)
    private Instant calledAt;

    @PrePersist
    void prePersist() {
        calledAt = Instant.now();
    }
}
