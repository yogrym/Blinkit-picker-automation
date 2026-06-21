package com.picker.BlinkitPicker.Services;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.picker.BlinkitPicker.Dto.OtpAuthRequest;
import com.picker.BlinkitPicker.Dto.OtpAuthRespons;
import com.picker.BlinkitPicker.Dto.VerifyOtpRequest;
import com.picker.BlinkitPicker.Dto.VerifyOtpRespons;
import com.picker.BlinkitPicker.Exception.CognitoException;
import com.picker.BlinkitPicker.Util.ContextDataUtil;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class WebClientServices {
        @Autowired
        private WebClient webClient;

        @Value("${cognito.clientId}")
        private String clientId;

        @Value("${cognito.poolId}")
        private String poolId;

        @Value("${cognito.url}")
        private String uri;

        public OtpAuthRespons sendOtpToUser(String phoneNumber) {
                String cognitoPhoneNumber = toIndianE164PhoneNumber(phoneNumber);

                OtpAuthRequest authBody = OtpAuthRequest.builder()
                                .authFlow("CUSTOM_AUTH")
                                .clientId(clientId)
                                .authParameters(OtpAuthRequest.AuthParameters.builder()
                                                .username(cognitoPhoneNumber)
                                                .build())
                                .clientMetadata(Map.of())
                                .userContextData(OtpAuthRequest.UserContextData.builder()
                                                .encodedData(ContextDataUtil.buildEncodedData(
                                                                clientId,
                                                                poolId,
                                                                cognitoPhoneNumber))
                                                .build())
                                .build();

                try {
                        return webClient.post()
                                        .uri(this.uri)
                                        .header("Content-Type", "application/x-amz-json-1.1")
                                        .header("x-amz-target", "AWSCognitoIdentityProviderService.InitiateAuth")
                                        .header("amz-sdk-invocation-id", UUID.randomUUID().toString())
                                        .bodyValue(authBody)
                                        .retrieve()
                                        .bodyToMono(OtpAuthRespons.class)
                                        .block();
                } catch (WebClientResponseException e) {
                        throw new CognitoException(e.getStatusCode().value(), e.getResponseBodyAsString());
                } catch (WebClientRequestException e) {
                        throw new CognitoException(503, "Something went wrong while connecting" + e.getMessage());
                }
        }

        public VerifyOtpRespons verifyOtp(String usernameUid, String answerAsOtp, String sessionString,
                        String phoneNumber) {
                String cognitoPhoneNumber = toIndianE164PhoneNumber(phoneNumber);

                try {
                        return webClient.post()
                                        .uri(this.uri)
                                        .header("Content-Type", "application/x-amz-json-1.1")
                                        .header("x-amz-target", "AWSCognitoIdentityProviderService.RespondToAuthChallenge")
                                        .header("amz-sdk-invocation-id", UUID.randomUUID().toString())
                                        .bodyValue(VerifyOtpRequest.builder()
                                                        .challengeName("CUSTOM_CHALLENGE")
                                                        .clientId(clientId)
                                                        .challengeResponses(
                                                                        VerifyOtpRequest.ChallengeResponses.builder()
                                                                                        .username(usernameUid)
                                                                                        .answer(answerAsOtp)
                                                                                        .phoneNumber(cognitoPhoneNumber)
                                                                                        .build())
                                                        .clientMetadata(Map.of())
                                                        .userContextData(VerifyOtpRequest.UserContextData.builder()
                                                                        .encodedData(ContextDataUtil.buildEncodedData(
                                                                                        clientId,
                                                                                        poolId,
                                                                                        usernameUid))
                                                                        .build())
                                                        .session(sessionString)
                                                        .build())
                                        .retrieve()
                                        .bodyToMono(VerifyOtpRespons.class)
                                        .block();
                } catch (WebClientResponseException e) {
                        throw new CognitoException(e.getStatusCode().value(), e.getResponseBodyAsString());
                } catch (WebClientRequestException e) {
                        throw new CognitoException(503, "Something went wrong while connecting: " + e.getMessage());
                }
        }

        private String toIndianE164PhoneNumber(String phoneNumber) {
                if (phoneNumber == null || phoneNumber.isBlank()) {
                        return phoneNumber;
                }

                String digitsOnly = phoneNumber.replaceAll("\\D", "");
                if (digitsOnly.startsWith("91") && digitsOnly.length() == 12) {
                        return "+" + digitsOnly;
                }

                return "+91" + digitsOnly;
        }

}
