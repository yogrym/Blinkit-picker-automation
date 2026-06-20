package com.picker.BlinkitPicker.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHeaderModel {

    private String authorization;

    @JsonProperty("content-type")
    private String contentType;

    private String accept;

    @JsonProperty("user-agent")
    private String userAgent;

    @JsonProperty("x-device-id")
    private String xDeviceId;

    private String employeeid;

    private String userid;

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
