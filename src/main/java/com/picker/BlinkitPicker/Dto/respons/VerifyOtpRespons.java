package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRespons {
    
    @JsonProperty("user")
    private User user;
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    private String message;
    private Boolean success;
    private Boolean verified;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        @JsonProperty("date_now")
        private String dateNow;
        private String id;
        private String phone;
    }
}
