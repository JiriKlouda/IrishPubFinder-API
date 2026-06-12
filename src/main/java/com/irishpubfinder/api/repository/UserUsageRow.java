package com.irishpubfinder.api.repository;

/** Native-query projection for one user's aggregated API usage over a period. */
public interface UserUsageRow {
    String getUserId();
    String getEmail();
    String getDisplayName();
    String getPhoneNumber();
    String getRole();
    long getNearbyGoogle();
    long getNearbyCache();
    long getDetailsGoogle();
    long getDetailsCache();
    long getAutocomplete();
    double getCost();
}
