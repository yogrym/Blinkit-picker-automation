package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckAvailableSlotsRequest {

    @JsonProperty("location_info")
    private LocationInfo locationInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationInfo {
        private Double latitude;
        
        private Double longitude;
        
        @JsonProperty("place_id")
        @Builder.Default
        private String placeId = "";
        
        @JsonProperty("place_name")
        @Builder.Default
        private String placeName = "";
    }
}
