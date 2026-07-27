package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @JsonProperty("user_phone")
    private String phoneNumber;

    @NotBlank
    @JsonProperty("lat")
    private double xLat;
    
    @NotBlank
    @JsonProperty("long")
    private double xLong;

}
