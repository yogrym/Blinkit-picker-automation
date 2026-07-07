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
        private String status = "All";

        @NotBlank
        @JsonProperty("location_info")
        private Location locationInfo;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Location {

            @JsonProperty("latitude")
            private Double xLat;

            @JsonProperty("longitude")
            private Double xLong;

            @JsonProperty("place_id")
            @Builder.Default
            private String placeId = "";

            @JsonProperty("place_name")
            @Builder.Default
            private String placeName = "";
        }
    }

