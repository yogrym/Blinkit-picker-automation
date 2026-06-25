package com.picker.BlinkitPicker.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FetchSlotsRequest {

        @NotBlank
        @JsonProperty("start_date")
        private String startDate;
        @NotBlank
        @JsonProperty("end_date")
        private String endDate;

        @Builder.Default
        private String status = "ALL";

        @NotBlank
        @JsonProperty("location_info")
        private Location locationInfo;

        private static class Location {

            @NotBlank
            @JsonProperty("latitude")
            private String xLat;

            @NotBlank
            @JsonProperty("longitude")
            private String xLong;

            @JsonProperty("place_id")
            private String placeId;

            @JsonProperty("place_name")
            private String placeName;
        }
    }

