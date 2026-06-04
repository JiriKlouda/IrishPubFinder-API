package com.irishpubfinder.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "badge_events",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "badge_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "badge_id", nullable = false)
    private String badgeId;

    @Column(name = "badge_name")
    private String badgeName;

    @Column(name = "badge_description", length = 500)
    private String badgeDescription;

    @Column(name = "badge_icon")
    private String badgeIcon;

    @Column(name = "badge_color", length = 12)
    private String badgeColor;

    @Column(name = "badge_category")
    private String badgeCategory;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    void prePersist() {
        earnedAt = LocalDateTime.now();
    }
}
