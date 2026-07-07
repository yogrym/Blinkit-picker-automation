package com.picker.BlinkitPicker.Dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpValidRespons {

    @JsonProperty("Username")
    private String username;

    @JsonProperty("Session")
    private String session;

    @JsonProperty("UserId")
    private String userId;
}
