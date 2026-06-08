package com.irishpubfinder.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "visits",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "place_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "place_id", nullable = false)
    private String placeId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;
    private Double rating;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    // Geo-enrichment computed client-side (where the polygon data lives)
    @Column(length = 4)
    private String continent;

    @Column(name = "irish_county", length = 64)
    private String irishCounty;

    @Column(name = "us_state", length = 64)
    private String usState;

    @Column(length = 128)
    private String city;

    @Column(name = "maps_url", length = 1024)
    private String mapsUrl;

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
