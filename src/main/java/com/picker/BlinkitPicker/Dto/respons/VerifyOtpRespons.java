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
    
    @JsonProperty("access_token")
    private String accessToken;

    private String message;
    private boolean success;
    private User user;
    private boolean verified;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @Data
    public static class User {
        @JsonProperty("date_now")
        private long dateNow;

        private String id;
        private String phone;
    }


}
