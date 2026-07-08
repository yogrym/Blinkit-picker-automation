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
public class OtpAuthRespons {

    @JsonProperty("ChallengeName")
    private String challengeName;

    @JsonProperty("ChallengeParameters")
    private ChallengeParameters challengeParameters;

    @JsonProperty("Session")
    private String session;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChallengeParameters {

        @JsonProperty("USERNAME")
        private String username;
    }
}
