package com.picker.BlinkitPicker.Dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.picker.BlinkitPicker.Enums.RoleEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetails {

    @JsonProperty("user_details")
    private UserData info;
    
    @JsonProperty("slot_information")
    private SlotInformation slotInformation;

    


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserData {
        @JsonProperty("store_id")
        private String storeId;
        
        @JsonProperty("store_name")
        private String storeName;

        @JsonProperty("employee_name")
        private String employeeName;

        @JsonProperty("employee_id")
        private String employeeId;

        @JsonProperty("total_booked_slots")
        private int totalBookedSlots;

        @JsonProperty("remaining_plan_validity")
        private String remainingPlanValidity;

        @JsonProperty("phone_number")
        private String phoneNumber;

        @JsonProperty("expires_at")
        private LocalDateTime expiresAt;

        @JsonProperty("role")
        private RoleEnum role;

        @JsonProperty("is_expired")
        private Boolean isExpired;

        @JsonProperty("is_blocked")
        private Boolean blocked;

        @JsonProperty("booking_sessions")
        private List<WorkerList.BookingData> bookingSessions;
    }

}
