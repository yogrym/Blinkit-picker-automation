package com.picker.BlinkitPicker.Dto.Internal_Respons;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LoginRespons {

    @JsonProperty("notification_client_id")
    private String notificationClientId;

    @JsonProperty("session_token")
    private String sessionToken;

    @JsonProperty("user_data")
    private UserData userData;

    private String message;

    @Data
    public static class UserData {

        @JsonProperty("role_details")
        private RoleDetails roleDetails;

        @JsonProperty("shift_details")
        private ShiftDetails shiftDetails;

        private String designation;

        @JsonProperty("cms_merchant_id")
        private Integer cmsMerchantId;

        @JsonProperty("city_id")
        private Integer cityId;

        @JsonProperty("city_name")
        private String cityName;

        @JsonProperty("site_name")
        private String siteName;

        private String name;

        private String phone;

        @JsonProperty("employee_type")
        private String employeeType;

        @JsonProperty("user_account_state")
        private String userAccountState;

        @JsonProperty("experience_type")
        private String experienceType;

        @JsonProperty("email_id")
        private String emailId;

        private Meta meta;

        @JsonProperty("date_of_joining")
        private String dateOfJoining;

        @JsonProperty("banner_details")
        private Map<String, Object> bannerDetails;

        @JsonProperty("is_profile_questions_enabled")
        private Boolean profileQuestionsEnabled;

        @JsonProperty("is_cash_management_enabled")
        private Boolean cashManagementEnabled;

        @JsonProperty("is_role_change_enable")
        private Boolean roleChangeEnabled;

        @JsonProperty("is_multi_device_login_blocked")
        private Boolean multiDeviceLoginBlocked;

        @JsonProperty("is_notification_service_call_enabled")
        private Boolean notificationServiceCallEnabled;

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
    public static class RoleDetails {

        private Integer id;

        @JsonProperty("role_names")
        private List<String> roleNames;

        @JsonProperty("user_id")
        private Integer userId;

        @JsonProperty("employee_id")
        private String employeeId;

        private String context;

        @JsonProperty("active_role_name")
        private String activeRoleName;

        @JsonProperty("active_site_id")
        private String activeSiteId;

        private List<String> permissions;
    }

    @Data
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
    public static class Meta {

        private List<Document> documents;

        private Preferences preferences;

        @JsonProperty("pan_verified")
        private String panVerified;

        @JsonProperty("onboarding_id")
        private String onboardingId;

        @JsonProperty("job_preference")
        private List<String> jobPreference;

        @JsonProperty("site_migration")
        private Map<String, Object> siteMigration;

        @JsonProperty("special_days_data")
        private SpecialDaysData specialDaysData;

        @JsonProperty("facility_reporting_time")
        private String facilityReportingTime;
    }

    @Data
    public static class Document {

        @JsonProperty("document_id")
        private String documentId;

        @JsonProperty("document_type")
        private String documentType;
    }

    @Data
    public static class Preferences {

        private String language;
    }

    @Data
    public static class SpecialDaysData {

        @JsonProperty("days_available")
        private List<String> daysAvailable;

        @JsonProperty("applied_config_id")
        private Integer appliedConfigId;

        @JsonProperty("milestones_achieved")
        private List<Integer> milestonesAchieved;
    }

    @Data
    public static class UserTrainingData {

        @JsonProperty("is_training_enabled")
        private Boolean trainingEnabled;
    }
}
