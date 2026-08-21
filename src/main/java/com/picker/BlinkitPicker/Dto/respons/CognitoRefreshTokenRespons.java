package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
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
    
    @Valid
    @JsonProperty("access_token")
    private String accessToken;
    @Valid
    @JsonProperty("refresh_token")
    private String refreshToken;
    private Boolean success;
    private String action;
}
