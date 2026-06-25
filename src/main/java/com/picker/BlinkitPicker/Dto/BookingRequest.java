package com.picker.BlinkitPicker.Dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public class BookingRequest {

    @NotBlank
    @JsonProperty("store_id")
    private Long storeId;

    @NotBlank
    @JsonProperty("dates")
    private List<String> dates;

}
