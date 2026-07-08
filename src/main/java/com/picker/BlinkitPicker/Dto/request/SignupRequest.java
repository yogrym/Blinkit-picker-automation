package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.picker.BlinkitPicker.Enums.RoleEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Plan is required")
    private String plan;

    private RoleEnum role;
}
