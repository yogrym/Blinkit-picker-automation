package com.picker.BlinkitPicker.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitoRefreshTokenRequest {

    @JsonProperty("AuthFlow")
    @Builder.Default
    private String authFlow = "REFRESH_TOKEN_AUTH";

    @JsonProperty("AuthParameters")
    private AuthParameters authParameters;

    @JsonProperty("ClientId")
    private String clientId;

    @JsonProperty("ClientMetadata")
    @Builder.Default
    private Map<String, String> clientMetadata = Map.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthParameters {

        @JsonProperty("REFRESH_TOKEN")
        private String refreshToken;
    }
}
