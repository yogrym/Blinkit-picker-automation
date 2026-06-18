package com.picker.BlinkitPicker.Dto;


import jakarta.validation.constraints.NotBlank;


import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String key;
}
