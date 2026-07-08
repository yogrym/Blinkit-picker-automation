package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SignupRespons {

    @JsonProperty("status")
    private String status;

    private String message;

}
