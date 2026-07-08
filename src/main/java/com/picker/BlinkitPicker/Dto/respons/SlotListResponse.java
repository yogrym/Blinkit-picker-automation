package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlotListResponse {

    private boolean success;
    private Data data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        @JsonProperty("max_slots_allowed")
        private int maxSlotsAllowed;

        private List<Store> stores;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Store {

        private String id;
        private String name;
        private String address;
        private double distance;
        private List<Slot> slots;

        @JsonProperty("booking_eligibility")
        private BookingEligibility bookingEligibility;

        /** Convenience: is this store itself eligible to receive bookings? */
        public boolean isEligible() {
            return bookingEligibility != null && bookingEligibility.isAllowed();
        }
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Slot {

        private long id;

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;

        @JsonProperty("is_booked")
        private boolean isBooked;

        @JsonProperty("is_cancellable")
        private boolean isCancellable;

        @JsonProperty("min_payout")
        private int minPayout;

        @JsonProperty("max_payout")
        private int maxPayout;

        @JsonProperty("site_id")
        private String siteId;

        @JsonProperty("booking_eligibility")
        private BookingEligibility bookingEligibility;

        /** Convenience: slot is open and allowed to be booked. */
        public boolean isAvailable() {
            return !isBooked && bookingEligibility != null && bookingEligibility.isAllowed();
        }
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookingEligibility {
        private boolean allowed;
    }
}
