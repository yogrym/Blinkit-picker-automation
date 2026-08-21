package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpClientRequest {
    
    @Valid
    @NotBlank
    @JsonProperty("user_phone")
    private String userPhone;
    
    @Valid
    @NotBlank
    @JsonProperty("verify_code")
    private String verifyCode;

    @NotNull
    @Valid
    @JsonProperty("x-lat")
    private Double xLat;
    
    @NotNull
    @Valid
    @JsonProperty("x-long")
    private Double Xlong;
}
