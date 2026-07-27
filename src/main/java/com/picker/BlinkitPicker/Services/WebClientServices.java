package com.picker.BlinkitPicker.Services;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.picker.BlinkitPicker.Dto.Internal_Request.AuthHeader;
import com.picker.BlinkitPicker.Dto.Internal_Request.BookSlotsRequest;
import com.picker.BlinkitPicker.Dto.Internal_Request.LoginRequestBody;
import com.picker.BlinkitPicker.Dto.Internal_Request.ViewAvailaibleSlotsRequest;
import com.picker.BlinkitPicker.Dto.Internal_Respons.LoginRespons;
import com.picker.BlinkitPicker.Dto.request.CognitoRefreshTokenRequest;
import com.picker.BlinkitPicker.Dto.request.FetchSlotsRequest;
import com.picker.BlinkitPicker.Dto.request.VerifyOtpRequest;
import com.picker.BlinkitPicker.Dto.respons.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Dto.respons.FetchSlotsResponse;
import com.picker.BlinkitPicker.Dto.respons.GlobalRespons;
import com.picker.BlinkitPicker.Dto.respons.OtpAuthRespons;
import com.picker.BlinkitPicker.Dto.respons.VerifyOtpRespons;
import com.picker.BlinkitPicker.Enums.ConfigEnums;
import com.picker.BlinkitPicker.Exception.CognitoException;
import com.picker.BlinkitPicker.Dto.respons.AvailableSlotsRespons;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Util.ContextDataUtil;
import com.picker.BlinkitPicker.cache.ConfigCache;

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

        private final ConfigCache configCache;

        public WebClientServices(ConfigCache configCache) {
                this.configCache = configCache;
        }

      
        public OtpAuthRespons sendOtpToUser(String phoneNumber,AuthHeader headers) {
                
                String sendOtpUrl = configCache.getAppCache().get(ConfigEnums.SEND_OTP);

                 MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                 body.add("user_phone", phoneNumber);
                 body.add("country_code", "91");

              /* OtpAuthRequest authBody = OtpAuthRequest.builder()
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
                                .build(); */  

                try {
                        return webClient.post()
                                        .uri(sendOtpUrl)
                                        /*
                                         .header("Content-Type", "application/x-amz-json-1.1")
                                        .header("x-amz-target", "AWSCognitoIdentityProviderService.InitiateAuth")
                                        .header("amz-sdk-invocation-id", UUID.randomUUID().toString())
                                        */
                                        .header("x-device-manufacturer", safeHeader(headers.getDeviceManufacturer()))
                                        .header("x-app-version-code", safeHeader(headers.getAppVersionCode()))
                                        .header("x-supply-apps-kit-version", safeHeader(headers.getSupplyAppsKitVersion()))
                                        .header("version_name", safeHeader(headers.getVersionName()))
                                        .header("app_client", safeHeader(headers.getAppClient()))
                                        .header("x-device-id", safeHeader(headers.getDeviceId()))
                                        .header("version_code", safeHeader(headers.getVersionCode()))
                                        .header("x-client-name", safeHeader(headers.getClientName()))
                                        .header("model", safeHeader(headers.getModel()))
                                        .header("x-device-hardware-type", safeHeader(headers.getDeviceHardwareType()))
                                        .header("x-app-version", safeHeader(headers.getAppVersion()))
                                        .header("version", safeHeader(headers.getVersion()))
                                        .header("x-app-theme", safeHeader(headers.getAppTheme()))
                                        .header("x-app-appearance", safeHeader(headers.getAppAppearance()))
                                        .header("x-system-appearance", safeHeader(headers.getSystemAppearance()))
                                        .header("x-accessibility-voice-over-enabled", safeHeader(headers.getAccessibilityVoiceOverEnabled()))
                                        .header("cookie", safeHeader(headers.getCookie()))
                                        .header("accept", safeHeader(headers.getAccept()))
                                        .header("access_token", safeHeader(headers.getAccessToken()))
                                        .header("x-app-locale", safeHeader(headers.getAppLocale()))
                                        .header("x-request-id", safeHeader(headers.getRequestId()))
                                        .header("requestid", safeHeader(headers.getRequestIdLower()))
                                        .header("x-api-key", safeHeader(headers.getApiKey()))
                                        .header("x-lat", String.valueOf(headers.getLattitude()))
                                        .header("x-long", String.valueOf(headers.getLongitude()))
                                        .header("x-gr-trace-id", safeHeader(headers.getGrTraceId()))
                                        .header("content-type", safeHeader(headers.getContentType()))
                                        .header("user-agent", safeHeader(headers.getUserAgent()))
                                        .header("accept-encoding", safeHeader(headers.getAcceptEncoding()))
                                        .header("priority", safeHeader(headers.getPriority()))
                                        .bodyValue(body)
                                        .retrieve()
                                        .bodyToMono(OtpAuthRespons.class)
                                        .block();
                } catch (WebClientResponseException e) {
                        throw new CognitoException(e.getStatusCode().value(), e.getResponseBodyAsString());
                } catch (WebClientRequestException e) {
                        throw new CognitoException(503, "Something went wrong while connecting" + e.getMessage());
                }
        }

        public VerifyOtpRespons verifyOtp(VerifyOtpRequest request,AuthHeader headers) {
               
                String verifyOtpUrl = configCache.getAppCache().get(ConfigEnums.VERIFY_OTP);

                MultiValueMap<String, String> verifyBody = new LinkedMultiValueMap<>();
                 verifyBody.add("user_phone", request.getUserNumber());
                 verifyBody.add("verify_code", request.getOtp());

                try {
                        return webClient.post()
                                        /*.uri(this.uri)
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
                                        .retrieve() */
                                        .uri(verifyOtpUrl)
                                        .header("x-device-manufacturer", safeHeader(headers.getDeviceManufacturer()))
                                        .header("x-app-version-code", safeHeader(headers.getAppVersionCode()))
                                        .header("x-supply-apps-kit-version", safeHeader(headers.getSupplyAppsKitVersion()))
                                        .header("version_name", safeHeader(headers.getVersionName()))
                                        .header("app_client", safeHeader(headers.getAppClient()))
                                        .header("x-device-id", safeHeader(headers.getDeviceId()))
                                        .header("version_code", safeHeader(headers.getVersionCode()))
                                        .header("x-client-name", safeHeader(headers.getClientName()))
                                        .header("model", safeHeader(headers.getModel()))
                                        .header("x-device-hardware-type", safeHeader(headers.getDeviceHardwareType()))
                                        .header("x-app-version", safeHeader(headers.getAppVersion()))
                                        .header("version", safeHeader(headers.getVersion()))
                                        .header("x-app-theme", safeHeader(headers.getAppTheme()))
                                        .header("x-app-appearance", safeHeader(headers.getAppAppearance()))
                                        .header("x-system-appearance", safeHeader(headers.getSystemAppearance()))
                                        .header("x-accessibility-voice-over-enabled", safeHeader(headers.getAccessibilityVoiceOverEnabled()))
                                        .header("cookie", safeHeader(headers.getCookie()))
                                        .header("accept", safeHeader(headers.getAccept()))
                                        .header("access_token", safeHeader(headers.getAccessToken()))
                                        .header("x-app-locale", safeHeader(headers.getAppLocale()))
                                        .header("x-request-id", safeHeader(headers.getRequestId()))
                                        .header("requestid", safeHeader(headers.getRequestIdLower()))
                                        .header("x-api-key", safeHeader(headers.getApiKey()))
                                        .header("x-lat", String.valueOf(headers.getLattitude()))
                                        .header("x-long", String.valueOf(headers.getLongitude()))
                                        .header("x-gr-trace-id", safeHeader(headers.getGrTraceId()))
                                        .header("content-type", safeHeader(headers.getContentType()))
                                        .header("user-agent", safeHeader(headers.getUserAgent()))
                                        .header("accept-encoding", safeHeader(headers.getAcceptEncoding()))
                                        .header("priority", safeHeader(headers.getPriority()))
                                        .bodyValue(verifyBody)
                                        .retrieve()
                                        .bodyToMono(VerifyOtpRespons.class)
                                        .block();
                } catch (WebClientResponseException e) {
                        throw new CognitoException(e.getStatusCode().value(), e.getResponseBodyAsString());
                } catch (WebClientRequestException e) {
                        throw new CognitoException(503, "Something went wrong while connecting: " + e.getMessage());
                }
        }


        /* 

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


        */


        public LoginRespons loginUser(AuthHeader headers, VerifyOtpRequest request) {

                String loginUrl = configCache.getAppCache().get(ConfigEnums.LOGIN);

                LoginRequestBody body = LoginRequestBody.builder()
                                .phone(request.getUserNumber())
                                .rfid(false)
                                .context("STOREOPS")
                                .build();

                return webClient.post()
                                .uri(loginUrl)
                                .header("x-device-manufacturer", safeHeader(headers.getDeviceManufacturer()))
                                        .header("x-app-version-code", safeHeader(headers.getAppVersionCode()))
                                        .header("x-supply-apps-kit-version", safeHeader(headers.getSupplyAppsKitVersion()))
                                        .header("version_name", safeHeader(headers.getVersionName()))
                                        .header("app_client", safeHeader(headers.getAppClient()))
                                        .header("x-device-id", safeHeader(headers.getDeviceId()))
                                        .header("version_code", safeHeader(headers.getVersionCode()))
                                        .header("x-client-name", safeHeader(headers.getClientName()))
                                        .header("model", safeHeader(headers.getModel()))
                                        .header("x-device-hardware-type", safeHeader(headers.getDeviceHardwareType()))
                                        .header("x-app-version", safeHeader(headers.getAppVersion()))
                                        .header("version", safeHeader(headers.getVersion()))
                                        .header("x-app-theme", safeHeader(headers.getAppTheme()))
                                        .header("x-app-appearance", safeHeader(headers.getAppAppearance()))
                                        .header("x-system-appearance", safeHeader(headers.getSystemAppearance()))
                                        .header("x-accessibility-voice-over-enabled", safeHeader(headers.getAccessibilityVoiceOverEnabled()))
                                        .header("cookie", safeHeader(headers.getCookie()))
                                        .header("accept", safeHeader(headers.getAccept()))
                                        .header("access_token", safeHeader(headers.getAccessToken()))
                                        .header("x-app-locale", safeHeader(headers.getAppLocale()))
                                        .header("x-request-id", safeHeader(headers.getRequestId()))
                                        .header("requestid", safeHeader(headers.getRequestIdLower()))
                                        .header("x-api-key", safeHeader(headers.getApiKey()))
                                        .header("x-lat", String.valueOf(headers.getLattitude()))
                                        .header("x-long", String.valueOf(headers.getLongitude()))
                                        .header("x-gr-trace-id", safeHeader(headers.getGrTraceId()))
                                        .header("content-type", safeHeader(headers.getContentType()))
                                        .header("user-agent", safeHeader(headers.getUserAgent()))
                                        .header("accept-encoding", safeHeader(headers.getAcceptEncoding()))
                                        .header("priority", safeHeader(headers.getPriority()))
                                        .bodyValue(body)
                                .retrieve()
                                .bodyToMono(LoginRespons.class)
                                .block();
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
                                .header("user-agent", userAgent != null ? userAgent
                                                : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
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
                                .header("user-agent", userAgent != null ? userAgent
                                                : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
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

        public Mono<AvailableSlotsRespons> getAvailableSlots(String cfBm, String requestId, String httpSessionToken,
                        String sessionToken, String siteId, UserModel user, ViewAvailaibleSlotsRequest viewAvailableSlotsRequest,
                        String jwt, AuthHeader headers) {
                
                String authHeader = jwt != null && jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;
                String availableSlotsUrl = configCache.getAppCache().get(ConfigEnums.AVAILAIABLE_SLOTS);

                return blinkClient.post()
                                .uri(availableSlotsUrl)
                                .header("requestid", requestId)
                                .header("cookie", "__cf_bm=" + cfBm)
                                .header("authorization", authHeader)
                                .header("user-agent", userAgent != null ? userAgent
                                                : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
                                .header("x-device-id", user.getUserHeaders().getXDeviceId())
                                .header("x-role", user.getRole() != null ? user.getRole().toString() : "PICKER")
                                .header("x-employeeid", user.getUserHeaders().getEmployeeId())
                                .header("x-lat", user.getUserHeaders().getXLat())
                                .header("x-long", user.getUserHeaders().getXLong())
                                .header("http_session_token", httpSessionToken)
                                .header("session-token", sessionToken)
                                .header("site-id", siteId)
                                .bodyValue(viewAvailableSlotsRequest)
                                .retrieve()
                                .bodyToMono(AvailableSlotsRespons.class);
        }

        private String safeHeader(String value) {
                return value != null ? value : "";
        }

}
