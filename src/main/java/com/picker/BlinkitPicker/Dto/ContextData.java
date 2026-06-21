package com.picker.BlinkitPicker.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextData {

    @JsonProperty("ApplicationName")
    private String applicationName;

    @JsonProperty("ApplicationTargetSdk")
    private String applicationTargetSdk;

    @JsonProperty("ApplicationVersion")
    private String applicationVersion;

    @JsonProperty("DeviceBrand")
    private String deviceBrand;

    @JsonProperty("DeviceFingerprint")
    private String deviceFingerprint;

    @JsonProperty("DeviceHardware")
    private String deviceHardware;

    @JsonProperty("DeviceName")
    private String deviceName;

    @JsonProperty("Product")
    private String product;

    @JsonProperty("BuildType")
    private String buildType;

    @JsonProperty("DeviceOsReleaseVersion")
    private String deviceOsReleaseVersion;

    @JsonProperty("DeviceSdkVersion")
    private String deviceSdkVersion;

    @JsonProperty("ClientTimezone")
    private String clientTimezone;

    @JsonProperty("Platform")
    private String platform;

    @JsonProperty("ThirdPartyDeviceId")
    private String thirdPartyDeviceId;

    @JsonProperty("DeviceId")
    private String deviceId;

    @JsonProperty("DeviceLanguage")
    private String deviceLanguage;

    @JsonProperty("ScreenHeightPixels")
    private String screenHeightPixels;

    @JsonProperty("ScreenWidthPixels")
    private String screenWidthPixels;
}
