package com.irishpubfinder.api.dto;

import com.irishpubfinder.api.model.BadgeEvent;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.model.Visit;

import java.time.LocalDateTime;

public class FeedItemDto {

    private String friendUserId;
    private String friendEmail;
    private String friendDisplayName;

    /** "visit" or "badge" */
    private String eventType;

    // Visit-specific fields (null for badge events)
    private String placeId;
    private String pubName;
    private String pubAddress;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String countryCode;

    // Badge-specific fields (null for visit events)
    private String badgeId;
    private String badgeName;
    private String badgeDescription;
    private String badgeIcon;
    private String badgeColor;
    private String badgeCategory;

    /** visitedAt for visits, earnedAt for badges — used for feed sorting */
    private LocalDateTime visitedAt;

    private FeedItemDto() {}

    public static FeedItemDto visitEvent(User user, Visit v) {
        FeedItemDto dto = new FeedItemDto();
        dto.friendUserId     = user.getId();
        dto.friendEmail      = user.getEmail();
        dto.friendDisplayName = user.getDisplayName();
        dto.eventType        = "visit";
        dto.placeId          = v.getPlaceId();
        dto.pubName          = v.getName();
        dto.pubAddress       = v.getAddress();
        dto.latitude         = v.getLatitude();
        dto.longitude        = v.getLongitude();
        dto.photoUrl         = v.getPhotoUrl();
        dto.countryCode      = v.getCountryCode();
        dto.visitedAt        = v.getCreatedAt();
        return dto;
    }

    public static FeedItemDto badgeEvent(User user, BadgeEvent be) {
        FeedItemDto dto = new FeedItemDto();
        dto.friendUserId      = user.getId();
        dto.friendEmail       = user.getEmail();
        dto.friendDisplayName = user.getDisplayName();
        dto.eventType         = "badge";
        dto.badgeId           = be.getBadgeId();
        dto.badgeName         = be.getBadgeName();
        dto.badgeDescription  = be.getBadgeDescription();
        dto.badgeIcon         = be.getBadgeIcon();
        dto.badgeColor        = be.getBadgeColor();
        dto.badgeCategory     = be.getBadgeCategory();
        dto.visitedAt         = be.getEarnedAt();
        return dto;
    }

    public String getFriendUserId()       { return friendUserId; }
    public String getFriendEmail()        { return friendEmail; }
    public String getFriendDisplayName()  { return friendDisplayName; }
    public String getEventType()          { return eventType; }
    public String getPlaceId()            { return placeId; }
    public String getPubName()            { return pubName; }
    public String getPubAddress()         { return pubAddress; }
    public Double getLatitude()           { return latitude; }
    public Double getLongitude()          { return longitude; }
    public String getPhotoUrl()           { return photoUrl; }
    public String getCountryCode()        { return countryCode; }
    public String getBadgeId()            { return badgeId; }
    public String getBadgeName()          { return badgeName; }
    public String getBadgeDescription()   { return badgeDescription; }
    public String getBadgeIcon()          { return badgeIcon; }
    public String getBadgeColor()         { return badgeColor; }
    public String getBadgeCategory()      { return badgeCategory; }
    public LocalDateTime getVisitedAt()   { return visitedAt; }
}
