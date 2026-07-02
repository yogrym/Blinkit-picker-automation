package com.picker.BlinkitPicker.Dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.picker.BlinkitPicker.Enums.RoleEnum;
import com.picker.BlinkitPicker.Dto.WorkerList.BookingData;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserListResponse {

    @JsonProperty("users")
    private List<UserData> users;

    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_users")
    private long totalUsers;

    @JsonProperty("has_next")
    private boolean hasNext;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserData {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("employee_name")
        private String employeeName;

        @JsonProperty("employee_id")
        private String employeeId;

        @JsonProperty("phone_number")
        private String phoneNumber;

        @JsonProperty("is_expired")
        private Boolean expired;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("expires_at")
        private LocalDateTime expiresAt;

        @JsonProperty("role")
        private RoleEnum role;

        @JsonProperty("total_booked_slots")
        private Long totalBookedSlots;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("api_key")
        private String apiKey;

        @JsonProperty("site_id")
        private String siteId;

        @JsonProperty("is_blocked")
        private Boolean blocked;

        @JsonProperty("booking_data_map")
        private List<BookingData> bookingDataMap;
    }
}
