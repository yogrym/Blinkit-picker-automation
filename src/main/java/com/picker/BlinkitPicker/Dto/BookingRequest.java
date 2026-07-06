package com.picker.BlinkitPicker.Dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class BookingRequest {

    @NotEmpty
    @JsonProperty("dates")
    @JsonAlias({"date", "dates", "date_filter", "dateFilter"})
    private List<String> dates;

    @NotEmpty
    @JsonProperty("time")
    @JsonAlias({"times", "time", "time_filter", "timeFilter"})
    private List<String> time;

}
