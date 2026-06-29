package com.picker.BlinkitPicker.Dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingRequest {

    @NotBlank
    @JsonProperty("dates")
    private List<String> dates;

    @NotBlank
    @JsonProperty
    private List<String> time;

}
