package com.picker.BlinkitPicker.Dto.Internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VewAvailaibleSlotsRequest {
    private LocationInfo locationInfo;

    public class LocationInfo {
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
