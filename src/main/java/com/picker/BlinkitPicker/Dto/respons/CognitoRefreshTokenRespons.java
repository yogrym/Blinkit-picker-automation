package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CognitoRefreshTokenRespons {

    @JsonProperty("AuthenticationResult")
    private AuthenticationResult authenticationResult;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthenticationResult {

        @JsonProperty("AccessToken")
        private String accessToken;

        @JsonProperty("ExpiresIn")
        private int expiresIn;

        @JsonProperty("IdToken")
        private String idToken;

        @JsonProperty("TokenType")
        private String tokenType;
    }
}
