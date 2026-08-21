package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class SendOtpRequest {

    @NotBlank
    @JsonProperty("user_phone")
    private String userPhone;
    
    @NotBlank
    @JsonProperty("country_code")
    private String countryCode;
    
    @jakarta.validation.constraints.NotNull
    private Location location;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        @JsonProperty("x-lat")
        private Double xLat;
        @JsonProperty("x-long")
        private Double xLong;
    }
}
