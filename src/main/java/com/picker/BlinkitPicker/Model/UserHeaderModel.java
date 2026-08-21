package com.picker.BlinkitPicker.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserHeaderModel {

    @Builder.Default
    @JsonProperty("x-device-manufacturer")
    private String deviceManufacturer = "VIVO";

    @Builder.Default
    @JsonProperty("x-app-version-code")
    private String appVersionCode = "162002";

    @Builder.Default
    @JsonProperty("x-supply-apps-kit-version")
    private String supplyAppsKitVersion = "3.79.0";

    @Builder.Default
    @JsonProperty("version_name")
    private String versionName = "16.20.2";

    @Builder.Default
    @JsonProperty("app_client")
    private String appClient = "android";

    @Builder.Default
    @JsonProperty("version_code")
    private String versionCode = "162002";

    @Builder.Default
    @JsonProperty("x-client-name")
    private String clientName = "storeops-app";

    @Builder.Default
    @JsonProperty("model")
    private String model = "I2404";

    @Builder.Default
    @JsonProperty("x-device-hardware-type")
    private String deviceHardwareType = "NON_HHD";

    @Builder.Default
    @JsonProperty("x-app-version")
    private String appVersion = "16.20.2";

    @Builder.Default
    @JsonProperty("version")
    private String version = "16.20.2";

    @Builder.Default
    @JsonProperty("x-app-theme")
    private String appTheme = "default";

    @Builder.Default
    @JsonProperty("x-app-appearance")
    private String appAppearance = "LIGHT";

    @Builder.Default
    @JsonProperty("x-system-appearance")
    private String systemAppearance = "UNSPECIFIED";

    @Builder.Default
    @JsonProperty("x-accessibility-voice-over-enabled")
    private String accessibilityVoiceOverEnabled = "0";

    @Builder.Default
    @JsonProperty("x-app-locale")
    private String appLocale = "en";

    @Builder.Default
    @JsonProperty("accept")
    private String accept = "application/json";

    @Builder.Default
    @JsonProperty("content-type")
    private String contentType = "application/x-www-form-urlencoded";

    @Builder.Default
    @JsonProperty("accept-encoding")
    private String acceptEncoding = "gzip, deflate, br";

    @Builder.Default
    @JsonProperty("priority")
    private String priority = "u=1, i";

    @Builder.Default
    @JsonProperty("user-agent")
    private String userAgent = "com.blinkitstoreops/162002 (Linux; U; Android 16; en; I2404; Build/BP2A.250605.031.A3_V000L1; Cronet/149.0.7827.159)";

    @JsonProperty("cookie")
    private String cookie;

    @JsonProperty("x-request-id")
    private String requestId;

    @JsonProperty("requestid")
    private String requestIdLower;

    @JsonProperty("x-gr-trace-id")
    private String grTraceId;

    @JsonProperty("x-api-key")
    private String apiKey;
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("x-device-id")
    private String xDeviceId;

    @JsonProperty("employeeid")
    private String employeeId;

    @JsonProperty("employee-name")
    private String employeeName;

    @JsonProperty("user-id")
    private Long userId;

    @JsonProperty("site-id")
    private String siteId;

    @JsonProperty("site-name")
    private String siteName;
    
    //set default as the picker for the both server and the client side
    private String role;

    @JsonProperty("http_session_token")
    private String userHttpSessionToken;

    @JsonProperty("session-token")
    private String userSessionToken;
    
    @JsonProperty("x-lat")
    private String xLat;

    @JsonProperty("x-long")
    private String xLong;
}
