package com.picker.BlinkitPicker.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GlobalRespons {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private globalData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class globalData {

        @JsonProperty("message")
        private String message;
        @JsonProperty("status_code")
        private int statusCode;
    }

}
