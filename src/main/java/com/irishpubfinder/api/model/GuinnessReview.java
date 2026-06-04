package com.irishpubfinder.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "guinness_reviews",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "place_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuinnessReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "place_id", nullable = false)
    private String placeId;

    @Column(nullable = false)
    private Integer creaminess;

    @Column(nullable = false)
    private Integer temperature;

    @Column(nullable = false)
    private Integer quality;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer overall;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
