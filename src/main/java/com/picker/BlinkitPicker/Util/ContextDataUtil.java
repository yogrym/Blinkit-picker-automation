package com.picker.BlinkitPicker.Util;

import com.picker.BlinkitPicker.Dto.ContextData;

import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ContextDataUtil {

    private static final Random random = new Random();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // List of static device profiles to ensure cohesive combinations
    private static final List<DeviceProfile> DEVICE_PROFILES = List.of(
            new DeviceProfile("OPPO", "OPPO/CPH1819/CPH1819:10/QP1A.190711.020/20240501000:user/release-keys", "mt6771",
                    "CPH1819", "CPH1819", "10", "29", "2068", "1080"),
            new DeviceProfile("OnePlus", "OnePlus/CPH2467/OP5913L1:13/TP1A.220905.001/S.202311021400:user/release-keys",
                    "qcom", "CPH2467", "CPH2467", "13", "33", "2400", "1080"),
            new DeviceProfile("samsung", "samsung/o1q/o1q:12/SP1A.210812.016/G991USQS5CWD3:user/release-keys", "qcom",
                    "SM-G991U", "o1q", "12", "31", "2400", "1080"),
            new DeviceProfile("Redmi", "Redmi/sweet/sweet:11/RKQ1.200826.002/V12.5.9.0.RKFMIXM:user/release-keys",
                    "qcom", "M2101K6G", "sweet", "11", "30", "2400", "1080"),
            new DeviceProfile("Google", "google/raven/raven:13/TQ3A.230901.001/10750268:user/release-keys", "gs101",
                    "GF5MQ", "raven", "13", "33", "3120", "1440"));

    /**
     * Generates a randomized ContextData instance.
     */
    public static ContextData getRandomContextData() {
        DeviceProfile profile = DEVICE_PROFILES.get(random.nextInt(DEVICE_PROFILES.size()));

        // Generate dynamic device ID: <uuid>:<epoch_ms>
        String deviceId = UUID.randomUUID().toString() + ":" + System.currentTimeMillis();

        ContextData contextData = new ContextData();

        // Hardcode static data
        contextData.setApplicationName("Store App");
        contextData.setApplicationTargetSdk("36");
        contextData.setApplicationVersion("15.54.6");
        contextData.setBuildType("user");
        contextData.setClientTimezone("05:30");
        contextData.setPlatform("ANDROID");
        contextData.setThirdPartyDeviceId("android_id");
        contextData.setDeviceLanguage("en");

        // Randomly pick device profile data
        contextData.setDeviceBrand(profile.brand);
        contextData.setDeviceFingerprint(profile.fingerprint);
        contextData.setDeviceHardware(profile.hardware);
        contextData.setDeviceName(profile.name);
        contextData.setProduct(profile.product);
        contextData.setDeviceOsReleaseVersion(profile.osReleaseVersion);
        contextData.setDeviceSdkVersion(profile.sdkVersion);
        contextData.setScreenHeightPixels(profile.screenHeight);
        contextData.setScreenWidthPixels(profile.screenWidth);

        // Generated device ID
        contextData.setDeviceId(deviceId);

        return contextData;
    }

    /**
     * Generates a new context and returns all its fields as a Map (JSON Map
     * format).
     */
    public static Map<String, Object> getAllContext() {
        ContextData contextData = getRandomContextData();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ApplicationName", contextData.getApplicationName());
        map.put("ApplicationTargetSdk", contextData.getApplicationTargetSdk());
        map.put("ApplicationVersion", contextData.getApplicationVersion());
        map.put("DeviceBrand", contextData.getDeviceBrand());
        map.put("DeviceFingerprint", contextData.getDeviceFingerprint());
        map.put("DeviceHardware", contextData.getDeviceHardware());
        map.put("DeviceName", contextData.getDeviceName());
        map.put("Product", contextData.getProduct());
        map.put("BuildType", contextData.getBuildType());
        map.put("DeviceOsReleaseVersion", contextData.getDeviceOsReleaseVersion());
        map.put("DeviceSdkVersion", contextData.getDeviceSdkVersion());
        map.put("ClientTimezone", contextData.getClientTimezone());
        map.put("Platform", contextData.getPlatform());
        map.put("ThirdPartyDeviceId", contextData.getThirdPartyDeviceId());
        map.put("DeviceId", contextData.getDeviceId());
        map.put("DeviceLanguage", contextData.getDeviceLanguage());
        map.put("ScreenHeightPixels", contextData.getScreenHeightPixels());
        map.put("ScreenWidthPixels", contextData.getScreenWidthPixels());

        return map;
    }

    public static String buildEncodedData(String clientId, String poolId, String username) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("contextData", getAllContext());
            payload.put("username", username);
            payload.put("userPoolId", poolId);
            payload.put("timestamp", String.valueOf(System.currentTimeMillis()));

            String payloadJson = objectMapper.writeValueAsString(payload);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(clientId.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hmacBytes);

            Map<String, Object> encodedData = new LinkedHashMap<>();
            encodedData.put("payload", payloadJson);
            encodedData.put("signature", signature);
            encodedData.put("version", "ANDROID20171114");

            String encodedDataJson = objectMapper.writeValueAsString(encodedData);
            return Base64.getEncoder().encodeToString(encodedDataJson.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            throw new RuntimeException("Failed to build Cognito encoded data", e);
        }
    }

    private static class DeviceProfile {
        String brand;
        String fingerprint;
        String hardware;
        String name;
        String product;
        String osReleaseVersion;
        String sdkVersion;
        String screenHeight;
        String screenWidth;

        DeviceProfile(String brand, String fingerprint, String hardware, String name, String product,
                String osReleaseVersion, String sdkVersion, String screenHeight, String screenWidth) {
            this.brand = brand;
            this.fingerprint = fingerprint;
            this.hardware = hardware;
            this.name = name;
            this.product = product;
            this.osReleaseVersion = osReleaseVersion;
            this.sdkVersion = sdkVersion;
            this.screenHeight = screenHeight;
            this.screenWidth = screenWidth;
        }
    }
}
