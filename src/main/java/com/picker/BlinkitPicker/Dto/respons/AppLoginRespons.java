package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppLoginRespons {
    
    private boolean success;
    private boolean verified;
    private String message;
    private ApplicationAuthorization aurtorization;



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationAuthorization {
        
        @JsonProperty("app_access_token")
        private String appAccessToken ;
        @JsonProperty("app_refresh_token")
        private String appRefreshToken;

    }

    
}


    