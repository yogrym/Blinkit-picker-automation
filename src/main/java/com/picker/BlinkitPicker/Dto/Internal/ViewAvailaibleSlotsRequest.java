package com.picker.BlinkitPicker.Dto.Internal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewAvailaibleSlotsRequest {

    private LocationInfo locationInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        @JsonProperty("latitude")
        private double xLat;
        @JsonProperty("longitude")
        private double xLong;
        @JsonProperty("place_id")
        private String placeId;
        @JsonProperty("place_name")
        private String placeName;
    }
}
