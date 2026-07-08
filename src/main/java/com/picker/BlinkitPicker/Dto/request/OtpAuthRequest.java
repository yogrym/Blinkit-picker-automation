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
public class OtpAuthRequest {

    @JsonProperty("AuthFlow")
    private String authFlow;

    @JsonProperty("AuthParameters")
    private AuthParameters authParameters;

    @JsonProperty("ClientId")
    private String clientId;

    @JsonProperty("ClientMetadata")
    private Map<String, String> clientMetadata;

    @JsonProperty("UserContextData")
    private UserContextData userContextData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthParameters {
        @JsonProperty("USERNAME")
        private String username;
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
