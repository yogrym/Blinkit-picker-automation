package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpClientRequest {

    @JsonProperty("Username")
    private String userName;

    @JsonProperty("Answer")
    private String answer;

    @JsonProperty("Session")
    private String session;

    @JsonProperty("UserId")
    private String userId;

    @JsonProperty("x_lat")
    private String xLat;

    @JsonProperty("x_long")
    private String xLong;
}
