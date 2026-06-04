package com.irishpubfinder.api.dto;

public class GuinnessRatingResponse {

    private String placeId;
    private double avgCreaminess;
    private double avgTemperature;
    private double avgQuality;
    private double avgPrice;
    private double avgOverall;
    private double overallScore;
    private int reviewCount;

    public GuinnessRatingResponse(String placeId, double avgCreaminess, double avgTemperature,
                                   double avgQuality, double avgPrice, double avgOverall,
                                   double overallScore, int reviewCount) {
        this.placeId = placeId;
        this.avgCreaminess = avgCreaminess;
        this.avgTemperature = avgTemperature;
        this.avgQuality = avgQuality;
        this.avgPrice = avgPrice;
        this.avgOverall = avgOverall;
        this.overallScore = overallScore;
        this.reviewCount = reviewCount;
    }

    public String getPlaceId() { return placeId; }
    public double getAvgCreaminess() { return avgCreaminess; }
    public double getAvgTemperature() { return avgTemperature; }
    public double getAvgQuality() { return avgQuality; }
    public double getAvgPrice() { return avgPrice; }
    public double getAvgOverall() { return avgOverall; }
    public double getOverallScore() { return overallScore; }
    public int getReviewCount() { return reviewCount; }
}
