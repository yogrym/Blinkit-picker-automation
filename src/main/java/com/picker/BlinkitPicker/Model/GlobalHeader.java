package com.picker.BlinkitPicker.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalHeader {
    @Builder.Default
    private String xDeviceManufacturer = "VIVO";

    @Builder.Default
    private String xAppVersionCode = "162002";

    @Builder.Default
    private String xSupplyAppsKitVersion = "3.79.0";

    @Builder.Default
    private String versionName = "16.20.2";

    @Builder.Default
    private String appClient = "android";

    @Builder.Default
    private String xDeviceId = "5a563dc4d76e2102";

    @Builder.Default
    private String versionCode = "162002";

    @Builder.Default
    private String xClientName = "storeops-app";

    @Builder.Default
    private String model = "I2404";

    @Builder.Default
    private String xDeviceHardwareType = "NON_HHD";

    @Builder.Default
    private String xAppVersion = "16.20.2";

    @Builder.Default
    private String version = "16.20.2";

    @Builder.Default
    private String xAppTheme = "default";

    @Builder.Default
    private String xAppAppearance = "LIGHT";

    @Builder.Default
    private String xSystemAppearance = "UNSPECIFIED";

    @Builder.Default
    private String xAccessibilityVoiceOverEnabled = "0";

    @Builder.Default
    private String cookie = "";

    @Builder.Default
    private String accept = "application/json";

    @Builder.Default
    private String accessToken = "";

    @Builder.Default
    private String xAppLocale = "en";

    @Builder.Default
    private String xRequestId = "";

    @Builder.Default
    private String requestId = "";

    @Builder.Default
    private String xApiKey = "";

    private String xLat;
    private String xLong;
    private String xGrTraceId;

    @Builder.Default
    private String contentType = "application/x-www-form-urlencoded";

    @Builder.Default
    private String userAgent = "com.blinkitstoreops/162002 (Linux; U; Android 16; en; I2404; Build/BP2A.250605.031.A3_V000L1; Cronet/149.0.7827.159)";

    @Builder.Default
    private String acceptEncoding = "gzip, deflate, br";

    @Builder.Default
    private String priority = "u=1, i";
}
