package com.picker.BlinkitPicker.Services;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.picker.BlinkitPicker.Dto.CognitoRefreshTokenRequest;
import com.picker.BlinkitPicker.Dto.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Dto.FetchSlotsRequest;
import com.picker.BlinkitPicker.Dto.FetchSlotsResponse;
import com.picker.BlinkitPicker.Dto.GlobalRespons;
import com.picker.BlinkitPicker.Dto.OtpAuthRequest;
import com.picker.BlinkitPicker.Dto.OtpAuthRespons;
import com.picker.BlinkitPicker.Dto.VerifyOtpRequest;
import com.picker.BlinkitPicker.Dto.VerifyOtpRespons;
import com.picker.BlinkitPicker.Dto.Internal.BookSlotsRequest;
import com.picker.BlinkitPicker.Exception.CognitoException;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Util.ContextDataUtil;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class WebClientServices {
        @Autowired
        @Qualifier("cognitoWebClient")
        private WebClient webClient;

        @Autowired
        @Qualifier("blinkitWebClient")
        private WebClient blinkClient;

        @Value("${cognito.clientId}")
        private String clientId;

        @Value("${list.slots.url}")
        private String listSlotsUrl;

        @Value("${book.slot.url}")
        private String bookSlotsUrl;

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
                                        .header("x-amz-target",
                                                        "AWSCognitoIdentityProviderService.RespondToAuthChallenge")
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

        public CognitoRefreshTokenRespons refreshToken(String refreshToken) {
                return webClient.post()
                                .uri(this.uri)
                                .header("Content-Type", "application/x-amz-json-1.1")
                                .header("x-amz-target", "AWSCognitoIdentityProviderService.InitiateAuth")
                                .header("amz-sdk-invocation-id", UUID.randomUUID().toString())
                                .bodyValue(CognitoRefreshTokenRequest.builder()
                                                .authFlow("REFRESH_TOKEN_AUTH")
                                                .clientId(clientId)
                                                .authParameters(CognitoRefreshTokenRequest.AuthParameters.builder()
                                                                .refreshToken(refreshToken)
                                                                .build())
                                                .build())
                                .retrieve()
                                .bodyToMono(CognitoRefreshTokenRespons.class)
                                .block();
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

        public Mono<FetchSlotsResponse> getSlotsDetails(String cfBm, String requestId, String jwt,
                        FetchSlotsRequest fetchSlotsRequest, String siteId, String employeeId, String userAgent,
                        String xDeviceId, String role, String sessionToken, String httpSessionToken) {

                System.out.println("[WebClient] Requesting getSlotsDetails with StoreID: " + siteId);

                String authHeader = jwt != null && jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;

                return blinkClient.post()
                                .uri(listSlotsUrl)
                                .header("requestid", requestId)
                                .header("cookie", "__cf_bm=" + cfBm)
                                .header("authorization", authHeader)
                                .header("user-agent", userAgent != null ? userAgent : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
                                .header("x-device-id", xDeviceId)
                                .header("x-role", role)
                                .header("x-employeeid", employeeId)
                                .header("x-lat", String.valueOf(fetchSlotsRequest.getLocationInfo().getXLat()))
                                .header("x-long", String.valueOf(fetchSlotsRequest.getLocationInfo().getXLong()))
                                .header("http_session_token", httpSessionToken)
                                .header("session-token", sessionToken)
                                .header("site-id", siteId)
                                .bodyValue(fetchSlotsRequest)
                                .retrieve()
                                .bodyToMono(FetchSlotsResponse.class)
                                .doOnNext(res -> System.out
                                                .println("[WebClient] getSlotsDetails SUCCESS response received"))
                                .doOnError(WebClientResponseException.class, e -> {
                                        System.out.println("[WebClient - ERROR] getSlotsDetails HTTP Status: "
                                                        + e.getStatusCode());
                                        System.out.println("[WebClient - ERROR] getSlotsDetails Response Body: "
                                                        + e.getResponseBodyAsString());
                                });
        }

        public Mono<GlobalRespons> bookSlots(String cfBm, String requestId, String jwt,
                        BookSlotsRequest bookSlotsRequest, String storeId, UserModel user,
                        String sessionToken, String httpSessionToken, String timesLog) {

                System.out.println("[WebClient] Requesting bookSlots for StoreID: " + storeId + " with slots: "
                                + bookSlotsRequest.getSlotIds() + " at times: " + timesLog);

                String authHeader = jwt != null && jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;
                String userAgent = user.getUserHeaders().getUserAgent();

                return blinkClient.post()
                                .uri(bookSlotsUrl)
                                .header("requestid", requestId)
                                .header("cookie", "__cf_bm=" + cfBm)
                                .header("authorization", authHeader)
                                .header("user-agent", userAgent != null ? userAgent : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
                                .header("x-device-id", user.getUserHeaders().getXDeviceId())
                                .header("x-role", user.getRole() != null ? user.getRole().toString() : "PICKER")
                                .header("x-employeeid", user.getUserHeaders().getEmployeeId())
                                .header("x-lat", user.getUserHeaders().getXLat())
                                .header("x-long", user.getUserHeaders().getXLong())
                                .header("http_session_token", httpSessionToken)
                                .header("session-token", sessionToken)
                                .header("site-id", storeId)
                                .bodyValue(bookSlotsRequest)
                                .retrieve()
                                .bodyToMono(GlobalRespons.class)
                                .doOnNext(res -> System.out.println("[WebClient] bookSlots SUCCESS response received"))
                                .doOnError(WebClientResponseException.class, e -> {
                                        System.out.println("[WebClient - ERROR] bookSlots HTTP Status: "
                                                        + e.getStatusCode());
                                        System.out.println("[WebClient - ERROR] bookSlots Response Body: "
                                                        + e.getResponseBodyAsString());
                                        System.out.println("[WebClient - ERROR] Failed to book slots: "
                                                        + bookSlotsRequest.getSlotIds() + " at times: " + timesLog);
                                });
        }

}
