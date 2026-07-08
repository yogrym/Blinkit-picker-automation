package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RenewPlanRequest {

    @JsonProperty("plan_type")
    private String planType;
}
