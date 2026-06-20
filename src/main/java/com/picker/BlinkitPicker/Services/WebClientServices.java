package com.picker.BlinkitPicker.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WebClientServices {
    @Autowired
    private WebClient webClient;

    public String sendOtpToUser(String uri) {
        return webClient.post()
                .uri(uri)
                .header("x-amz-target", "AWS.CognitoIdentityService.InitiateAuth")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
