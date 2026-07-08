package com.picker.BlinkitPicker.Dto.request;

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
public class VerifyOtpRequest {

    @JsonProperty("ChallengeName")
    private String challengeName;

    @JsonProperty("ChallengeResponses")
    private ChallengeResponses challengeResponses;

    @JsonProperty("ClientId")
    private String clientId;

    @JsonProperty("ClientMetadata")
    private Map<String, String> clientMetadata;

    @JsonProperty("Session")
    private String session;

    @JsonProperty("UserContextData")
    private UserContextData userContextData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChallengeResponses {

        @JsonProperty("USERNAME")
        private String username;

        @JsonProperty("ANSWER")
        private String answer;

        @JsonProperty("userAttributes.phone_number")
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserContextData {

        @JsonProperty("EncodedData")
        private String encodedData;
    }
}
