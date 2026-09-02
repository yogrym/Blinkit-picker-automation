package com.picker.BlinkitPicker.Dto;

import java.util.List;

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
public class SlotInformation {
    private SlotData data;
    private boolean success;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlotData {
        private List<SlotDate> dates;
        
        @JsonProperty("allow_location_change")
        private boolean allowLocationChange;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SlotDate {
        private String date;
        private String title;
        private String subtitle;
        
        @JsonProperty("is_enabled")
        private boolean isEnabled;
        
        @JsonProperty("is_booked")
        private boolean isBooked;
        
        private Chip chip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Chip {
        private String icon;
        
        @JsonProperty("icon_code")
        private String iconCode;
        
        private String text;
        private String type;
        private String color;
        private String background;
    }
}
