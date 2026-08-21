package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    
    @Valid
    @NotBlank
    @JsonProperty("phone")
    private String phone;

    @JsonProperty("rfid_supported")
    private Boolean rfIdSupported;

    @JsonProperty("context")
    private String context;
}
