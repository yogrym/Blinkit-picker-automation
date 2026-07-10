package com.picker.BlinkitPicker.Dto.respons;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotsRespons {

    @JsonProperty("data")
    private DataInfo data;

    @JsonProperty("success")
    private boolean success;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataInfo {
        @JsonProperty("dates")
        private List<DateInfo> dates;

        @JsonProperty("location_info")
        private LocationInfo locationInfo;

        @JsonProperty("allow_location_change")
        private boolean allowLocationChange;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DateInfo {
        @JsonProperty("date")
        private String date;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subtitle")
        private String subtitle;

        @JsonProperty("is_enabled")
        private boolean isEnabled;

        @JsonProperty("is_booked")
        private boolean isBooked;

        @JsonProperty("chip")
        private ChipInfo chip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChipInfo {
        @JsonProperty("icon")
        private String icon;

        @JsonProperty("icon_code")
        private String iconCode;

        @JsonProperty("text")
        private String text;

        @JsonProperty("type")
        private String type;

        @JsonProperty("color")
        private String color;

        @JsonProperty("background")
        private String background;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationInfo {
        @JsonProperty("latitude")
        private double latitude;

        @JsonProperty("longitude")
        private double longitude;

        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("place_name")
        private String placeName;
    }
}
