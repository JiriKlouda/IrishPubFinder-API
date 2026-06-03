package com.irishpubfinder.api.dto;

import java.time.LocalDateTime;

public class FeedItemDto {
    private String friendUserId;
    private String friendEmail;
    private String friendDisplayName;
    private String placeId;
    private String pubName;
    private String pubAddress;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private LocalDateTime visitedAt;

    public FeedItemDto(String friendUserId, String friendEmail, String friendDisplayName,
                       String placeId, String pubName, String pubAddress,
                       Double latitude, Double longitude, String photoUrl,
                       LocalDateTime visitedAt) {
        this.friendUserId = friendUserId;
        this.friendEmail = friendEmail;
        this.friendDisplayName = friendDisplayName;
        this.placeId = placeId;
        this.pubName = pubName;
        this.pubAddress = pubAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoUrl = photoUrl;
        this.visitedAt = visitedAt;
    }

    public String getFriendUserId() { return friendUserId; }
    public String getFriendEmail() { return friendEmail; }
    public String getFriendDisplayName() { return friendDisplayName; }
    public String getPlaceId() { return placeId; }
    public String getPubName() { return pubName; }
    public String getPubAddress() { return pubAddress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getPhotoUrl() { return photoUrl; }
    public LocalDateTime getVisitedAt() { return visitedAt; }
}
