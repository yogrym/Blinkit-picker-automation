package com.picker.BlinkitPicker.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @JsonProperty("phone_number")
    private String phoneNumber;
}
