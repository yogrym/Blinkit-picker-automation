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
public class VerifyOtpClientRequest {

    @JsonProperty("Username")
    private String userName;

    @JsonProperty("Answer")
    private String answer;

    @JsonProperty("Session")
    private String session;

    @JsonProperty("UserId")
    private String userId;
}
