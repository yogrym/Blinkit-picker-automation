package com.picker.BlinkitPicker.Dto.respons;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccefullLoginResponse {

    @JsonProperty("notification_client_id")
    private String notificationClientId;

    @JsonProperty("session_token")
    private String sessionToken;

    @JsonProperty("user_data")
    private UserData userData;

    @JsonProperty("message")
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserData {

        @JsonProperty("role_details")
        private RoleDetails roleDetails;

        @JsonProperty("shift_details")
        private ShiftDetails shiftDetails;

        @JsonProperty("designation")
        private String designation;

        @JsonProperty("cms_merchant_id")
        private Long cmsMerchantId;

        @JsonProperty("city_id")
        private Integer cityId;

        @JsonProperty("city_name")
        private String cityName;

        @JsonProperty("site_name")
        private String siteName;

        @JsonProperty("name")
        private String name;

        @JsonProperty("phone")
        private String phone;

        @JsonProperty("employee_type")
        private String employeeType;

        @JsonProperty("user_account_state")
        private String userAccountState;

        @JsonProperty("experience_type")
        private String experienceType;

        @JsonProperty("email_id")
        private String emailId;

        @JsonProperty("meta")
        private Meta meta;

        @JsonProperty("date_of_joining")
        private String dateOfJoining;

        @JsonProperty("banner_details")
        private Object bannerDetails;

        @JsonProperty("is_profile_questions_enabled")
        private Boolean profileQuestionsEnabled;

        @JsonProperty("is_cash_management_enabled")
        private Boolean cashManagementEnabled;

        @JsonProperty("is_role_change_enable")
        private Boolean roleChangeEnable;

        @JsonProperty("is_multi_device_login_blocked")
        private Boolean multiDeviceLoginBlocked;

        @JsonProperty("is_notification_service_call_enabled")
        private Boolean notificationServiceCallEnabled;

        @JsonProperty("preferences")
        private Preferences preferences;

        @JsonProperty("payouts_enabled")
        private Boolean payoutsEnabled;

        @JsonProperty("leaderboard_enabled")
        private Boolean leaderboardEnabled;

        @JsonProperty("leaderboard_milestones_enabled")
        private Boolean leaderboardMilestonesEnabled;

        @JsonProperty("variable_pay_enabled")
        private Boolean variablePayEnabled;

        @JsonProperty("user_training_data")
        private UserTrainingData userTrainingData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDetails {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("role_names")
        private List<String> roleNames;

        @JsonProperty("user_id")
        private Long userId;

        @JsonProperty("employee_id")
        private String employeeId;

        @JsonProperty("context")
        private String context;

        @JsonProperty("active_role_name")
        private String activeRoleName;

        @JsonProperty("active_site_id")
        private String activeSiteId;

        @JsonProperty("permissions")
        private List<String> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftDetails {

        @JsonProperty("is_shift_active")
        private Boolean shiftActive;

        @JsonProperty("prev_end_shift_time")
        private String prevEndShiftTime;

        @JsonProperty("prev_end_shift_method")
        private String prevEndShiftMethod;

        @JsonProperty("shift_start_time")
        private String shiftStartTime;

        @JsonProperty("shift_ids")
        private List<String> shiftIds;

        @JsonProperty("total_seconds_worked_today")
        private Integer totalSecondsWorkedToday;

        @JsonProperty("total_active_seconds_worked_today")
        private Integer totalActiveSecondsWorkedToday;

        @JsonProperty("is_break_active")
        private Boolean breakActive;

        @JsonProperty("total_break_today")
        private Integer totalBreakToday;

        @JsonProperty("total_active_seconds")
        private Integer totalActiveSeconds;

        @JsonProperty("frs_config")
        private Object frsConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {

        @JsonProperty("documents")
        private List<Document> documents;

        @JsonProperty("preferences")
        private Preferences preferences;

        @JsonProperty("pan_verified")
        private String panVerified;

        @JsonProperty("onboarding_id")
        private String onboardingId;

        @JsonProperty("job_preference")
        private List<String> jobPreference;

        @JsonProperty("site_migration")
        private Object siteMigration;

        @JsonProperty("special_days_data")
        private SpecialDaysData specialDaysData;

        @JsonProperty("facility_reporting_time")
        private String facilityReportingTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {

        @JsonProperty("document_id")
        private String documentId;

        @JsonProperty("document_type")
        private String documentType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Preferences {

        @JsonProperty("language")
        private String language;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialDaysData {

        @JsonProperty("days_available")
        private List<String> daysAvailable;

        @JsonProperty("applied_config_id")
        private Long appliedConfigId;

        @JsonProperty("milestones_achieved")
        private List<Integer> milestonesAchieved;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserTrainingData {

        @JsonProperty("is_training_enabled")
        private Boolean trainingEnabled;
    }
}
