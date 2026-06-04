package com.irishpubfinder.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GuinnessReviewRequest {

    @NotBlank
    private String placeId;

    @NotNull @Min(1) @Max(5)
    private Integer creaminess;

    @NotNull @Min(1) @Max(5)
    private Integer temperature;

    @NotNull @Min(1) @Max(5)
    private Integer quality;

    @NotNull @Min(1) @Max(5)
    private Integer price;

    @NotNull @Min(1) @Max(5)
    private Integer overall;

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }
    public Integer getCreaminess() { return creaminess; }
    public void setCreaminess(Integer creaminess) { this.creaminess = creaminess; }
    public Integer getTemperature() { return temperature; }
    public void setTemperature(Integer temperature) { this.temperature = temperature; }
    public Integer getQuality() { return quality; }
    public void setQuality(Integer quality) { this.quality = quality; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getOverall() { return overall; }
    public void setOverall(Integer overall) { this.overall = overall; }
}
