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

    @JsonProperty("AuthenticationResult")
    private AuthenticationResult authenticationResult;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticationResult {

        @JsonProperty("AccessToken")
        private String accessToken;

        @JsonProperty("IdToken")
        private String idToken;

        @JsonProperty("RefreshToken")
        private String refreshToken;

        @JsonProperty("ExpiresIn")
        private Integer expiresIn;

        @JsonProperty("TokenType")
        private String tokenType;
    }
}
