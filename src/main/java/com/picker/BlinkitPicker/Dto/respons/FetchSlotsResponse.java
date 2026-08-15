package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchSlotsResponse {

    private boolean success;
    private DataBlock data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBlock {
        @JsonProperty("max_slots_allowed")
        private int maxSlotsAllowed;

        @JsonProperty("training_details")
        private TrainingDetails trainingDetails;

        private List<Store> stores;

        @JsonProperty("filter_sort_options")
        private FilterSortOptions filterSortOptions;

        @JsonProperty("extra_info")
        private Object extraInfo;

        @JsonProperty("location_info")
        private LocationInfo locationInfo;

        @JsonProperty("allow_location_change")
        private boolean allowLocationChange;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrainingDetails {
        @JsonProperty("has_training_slots")
        private boolean hasTrainingSlots;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Store {
        private String id;
        private String name;
        private String address;
        private double distance;
        private List<Slot> slots;

        @JsonProperty("cancellation_warning")
        private String cancellationWarning;

        @JsonProperty("is_first_visit")
        private boolean isFirstVisit;

        @JsonProperty("is_preferred_site")
        private boolean isPreferredSite;

        @JsonProperty("booking_eligibility")
        private BookingEligibility bookingEligibility;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Slot {
        private long id;

        @JsonProperty("parent_id")
        private long parentId;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;

        @JsonProperty("is_booked")
        private boolean isBooked;

        @JsonProperty("is_cancellable")
        private boolean isCancellable;

        @JsonProperty("sub_string")
        private SubString subString;

        @JsonProperty("min_payout")
        private int minPayout;

        @JsonProperty("max_payout")
        private int maxPayout;

        @JsonProperty("slot_type")
        private String slotType;

        @JsonProperty("site_id")
        private String siteId;

        @JsonProperty("booking_eligibility")
        private BookingEligibility bookingEligibility;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubString {
        private String text;
        private String color;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookingEligibility {
        private boolean allowed;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterSortOptions {
        @JsonProperty("sort_options")
        private List<SortOption> sortOptions;

        @JsonProperty("filter_options")
        private List<FilterOption> filterOptions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SortOption {
        private String title;
        private String id;
        @JsonProperty("is_selected")
        private boolean isSelected;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterOption {
        private String title;
        private String id;
        @JsonProperty("is_selected")
        private boolean isSelected;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationInfo {
        private double latitude;
        private double longitude;
        @JsonProperty("place_id")
        private String placeId;
        @JsonProperty("place_name")
        private String placeName;
    }

    public Object getErrorCode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getErrorCode'");
    }
}
