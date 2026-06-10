package com.irishpubfinder.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "api_call_logs", indexes = {
        @Index(name = "idx_api_call_logs_called_at", columnList = "called_at")
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

    @Column(name = "called_at", nullable = false, updatable = false)
    private Instant calledAt;

    @PrePersist
    void prePersist() {
        calledAt = Instant.now();
    }
}
