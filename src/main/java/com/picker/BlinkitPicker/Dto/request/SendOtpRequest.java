package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank
    @JsonProperty("user_phone")
    private String userPhone;
    
    @NotBlank
    @JsonProperty("country_code")
    private String countryCode;
    
    @NotBlank
    private Location location;

    @Data
    @Builder
    public static class Location {
        @JsonProperty("x-lat")
        private Double xLat;
        @JsonProperty("x-long")
        private Double xLong;
    }
}
