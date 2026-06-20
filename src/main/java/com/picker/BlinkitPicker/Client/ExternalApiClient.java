package com.picker.BlinkitPicker.Client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ExternalApiClient {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/x-amz-json-1.1")
                .defaultHeader("x-amz-user-agent", "aws-sdk-kotlin/1.3.81")
                .defaultHeader("accept-encoding", "identity")
                .defaultHeader("amz-sdk-request", "attempt=1; max=3")
                .defaultHeader("user-agent",
                        "aws-sdk-kotlin/1.3.81 ua/2.1 api/cognito-identity-provider#1.3.81 os/android#4.14.141+ lang/kotlin#2.1.21 md/javaVersion#0 md/jvmName#Dalvik md/jvmVersion#2.1.0 md/androidApiVersion#29 md/androidRelease#10 lib/amplify-android#2.27.4 md/locale#en_GB")
                .build();
    }
}
