package com.picker.BlinkitPicker.Client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class ExternalApiClient {

    private static final MediaType AWS_JSON = MediaType.valueOf("application/x-amz-json-1.1");

    @Bean
    public WebClient webClient(JsonMapper jsonMapper) {

        // Cognito uses AWS JSON, which Spring should encode/decode as normal JSON.
        ExchangeFilterFunction awsContentTypeRewriter = ExchangeFilterFunction.ofResponseProcessor(
                response -> Mono.just(
                        response.mutate()
                                .headers(h -> h.setContentType(MediaType.APPLICATION_JSON))
                                .build()));

        return WebClient.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jacksonJsonEncoder(
                            new JacksonJsonEncoder(jsonMapper, AWS_JSON, MediaType.APPLICATION_JSON));
                    configurer.defaultCodecs().jacksonJsonDecoder(
                            new JacksonJsonDecoder(jsonMapper, AWS_JSON, MediaType.APPLICATION_JSON));
                })
                .filter(awsContentTypeRewriter)
                .defaultHeader("Content-Type", AWS_JSON.toString())
                .defaultHeader("x-amz-user-agent", "aws-sdk-kotlin/1.3.81")
                .defaultHeader("accept-encoding", "identity")
                .defaultHeader("amz-sdk-request", "attempt=1; max=3")
                .defaultHeader("user-agent",
                        "aws-sdk-kotlin/1.3.81 ua/2.1 api/cognito-identity-provider#1.3.81 os/android#4.14.141+ lang/kotlin#2.1.21 md/javaVersion#0 md/jvmName#Dalvik md/jvmVersion#2.1.0 md/androidApiVersion#29 md/androidRelease#10 lib/amplify-android#2.27.4 md/locale#en_GB")
                .build();
    }
}
