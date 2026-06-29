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

    private String authorization;

    @JsonProperty("content-type")
    private String contentType;

    private String accept;

    @JsonProperty("user-agent")
    @Builder.Default
    private String userAgent = "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)";

    @JsonProperty("x-device-id")
    private String xDeviceId;

    @JsonProperty("employeeid")
    private String employeeId;

    @JsonProperty("employee-name")
    private String employeeName;

    @JsonProperty("user-id")
    private String userId;

    @JsonProperty("site-id")
    private String siteId;

    private String role;

    @JsonProperty("http_session_token")
    private String httpSessionToken;

    @JsonProperty("session-token")
    private String sessionToken;

    @JsonProperty("x-lat")
    private String xLat;

    @JsonProperty("x-long")
    private String xLong;
}
