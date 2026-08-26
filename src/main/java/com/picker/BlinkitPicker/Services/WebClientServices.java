package com.picker.BlinkitPicker.Services;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import com.picker.BlinkitPicker.Cache.AppCahe;
import com.picker.BlinkitPicker.Dto.Internal.BookSlotsRequest;
import com.picker.BlinkitPicker.Dto.Internal.ViewAvailaibleSlotsRequest;
import com.picker.BlinkitPicker.Dto.request.CognitoRefreshTokenRequest;
import com.picker.BlinkitPicker.Dto.request.FetchSlotsRequest;
import com.picker.BlinkitPicker.Dto.request.LoginRequest;
import com.picker.BlinkitPicker.Dto.request.OtpAuthRequest;
import com.picker.BlinkitPicker.Dto.request.SendOtpRequest;
import com.picker.BlinkitPicker.Dto.request.VerifyOtpClientRequest;
import com.picker.BlinkitPicker.Dto.request.VerifyOtpRequest;
import com.picker.BlinkitPicker.Dto.respons.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Dto.respons.FetchSlotsResponse;
import com.picker.BlinkitPicker.Dto.respons.GlobalRespons;
import com.picker.BlinkitPicker.Dto.respons.SuccefullLoginResponse;
import com.picker.BlinkitPicker.Dto.respons.SuccessfullOtpResponse;
import com.picker.BlinkitPicker.Dto.respons.VerifyOtpRespons;
import com.picker.BlinkitPicker.Enums.ApiEnums;
import com.picker.BlinkitPicker.Exception.CognitoException;
import com.picker.BlinkitPicker.Dto.respons.AvailableSlotsRespons;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Util.ContextDataUtil;
import com.picker.BlinkitPicker.Util.GenerateCookie;

import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class WebClientServices {


        @Autowired
        private AppCahe appCahe;
        @Autowired
        @Qualifier("cognitoWebClient")
        private WebClient webClient;

        @Autowired
        @Qualifier("blinkitWebClient")
        private WebClient blinkClient;

    

        public SuccessfullOtpResponse sendOtpToUser(MultiValueMap<String, String> userData, UserHeaderModel headers,SendOtpRequest request) {
              

                try {
                        return webClient.post()
                                        .uri(appCahe.getApiUrl(ApiEnums.SEND_OTP))
                                        .headers(httpHeaders -> {
                                                httpHeaders.set("Content-Type", "application/x-www-form-urlencoded");
                                                httpHeaders.set("x-device-manufacturer", headers.getDeviceManufacturer());
                                                httpHeaders.set("x-app-version-code", headers.getAppVersionCode());
                                                httpHeaders.set("x-supply-apps-kit-version", headers.getSupplyAppsKitVersion());
                                                httpHeaders.set("version_name", headers.getVersionName());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-device-id", headers.getXDeviceId());
                                                httpHeaders.set("version_code", headers.getVersionCode());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-client-name", headers.getClientName());
                                                httpHeaders.set("model", headers.getModel());
                                                httpHeaders.set("x-device-hardware-type", headers.getDeviceHardwareType());
                                                httpHeaders.set("x-app-version", headers.getAppVersion());
                                                httpHeaders.set("version", headers.getVersion());
                                                httpHeaders.set("x-app-theme", headers.getAppTheme());
                                                httpHeaders.set("x-app-appearance", headers.getAppAppearance());
                                                httpHeaders.set("user-agent", safe(headers.getUserAgent()));
                                                httpHeaders.set("x-system-appearance", safe(headers.getAppAppearance()));
                                                httpHeaders.set("x-system-theme", safe(headers.getAppTheme()));
                                                httpHeaders.set("x-lat", request.getLocation().getXLat().toString());
                                                httpHeaders.set("x-long", request.getLocation().getXLong().toString());
                                                httpHeaders.set("accept", "application/json");
                                                httpHeaders.set("x-api-key", "b30153b3-a5f8-4118-9af9-43d05487c1b3");
                                                httpHeaders.set("x-grace-trace-id", safe(headers.getGrTraceId()));
                                                httpHeaders.set("requestid", safe(headers.getRequestId()));
                                                httpHeaders.set("x-request-id", safe(headers.getRequestId()));
                                                httpHeaders.set("cookie", safe(headers.getCookie()));
                                                httpHeaders.set("x-app-locale", safe(headers.getAppLocale()));
                                                httpHeaders.set("access_token","");
                                                httpHeaders.set("priority", safe(headers.getPriority()));
                                        })
                                        .body(BodyInserters.fromFormData(userData))
                                        .retrieve()
                                        .bodyToMono(SuccessfullOtpResponse.class)
                                        .block();
                } catch (WebClientResponseException e) {
                        throw new CognitoException(e.getStatusCode().value(), e.getResponseBodyAsString());
                } catch (WebClientRequestException e) {
                        throw new CognitoException(503, "Something went wrong while connecting" + e.getMessage());
                }
        }

        private String safe(String value) {
                return value == null ? "" : value;
        }

        public VerifyOtpRespons verifyOtp(MultiValueMap<String,String> requestBodyData, UserHeaderModel headers,VerifyOtpClientRequest request) {
               
                String xTrace = GenerateCookie.generateRequestId(); // for all kind of the requestid we will use it


                try {
                        return webClient.post()
                                        .uri(appCahe.getApiUrl(ApiEnums.VERIFY_OTP))
                                         .headers(httpHeaders -> {
                                                httpHeaders.set("Content-Type", "application/x-www-form-urlencoded");
                                                httpHeaders.set("x-device-manufacturer", headers.getDeviceManufacturer());
                                                httpHeaders.set("x-app-version-code", headers.getAppVersionCode());
                                                httpHeaders.set("x-supply-apps-kit-version", headers.getSupplyAppsKitVersion());
                                                httpHeaders.set("version_name", headers.getVersionName());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-device-id", headers.getXDeviceId());
                                                httpHeaders.set("version_code", headers.getVersionCode());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-client-name", headers.getClientName());
                                                httpHeaders.set("model", headers.getModel());
                                                httpHeaders.set("x-device-hardware-type", headers.getDeviceHardwareType());
                                                httpHeaders.set("x-app-version", headers.getAppVersion());
                                                httpHeaders.set("version", headers.getVersion());
                                                httpHeaders.set("x-app-theme", headers.getAppTheme());
                                                httpHeaders.set("x-app-appearance", headers.getAppAppearance());
                                                httpHeaders.set("user-agent", safe(headers.getUserAgent()));
                                                httpHeaders.set("x-system-appearance", safe(headers.getAppAppearance()));
                                                httpHeaders.set("x-system-theme", safe(headers.getAppTheme()));
                                                httpHeaders.set("x-lat", request.getXLat().toString());
                                                httpHeaders.set("x-long", request.getXlong().toString());
                                                httpHeaders.set("accept", "application/json");
                                                httpHeaders.set("x-api-key", "b30153b3-a5f8-4118-9af9-43d05487c1b3");
                                                httpHeaders.set("x-grace-trace-id", xTrace);
                                                httpHeaders.set("requestid", xTrace);
                                                httpHeaders.set("x-request-id", xTrace);
                                                httpHeaders.set("cookie", safe(headers.getCookie()));
                                                httpHeaders.set("x-app-locale", safe(headers.getAppLocale()));
                                                httpHeaders.set("access_token","");
                                                httpHeaders.set("priority", safe(headers.getPriority()));
                                        })
                                        .body(BodyInserters.fromFormData(requestBodyData))
                                        .retrieve()
                                        .bodyToMono(VerifyOtpRespons.class)
                                        .block();
                } catch (WebClientResponseException e) {
                        throw new CognitoException(e.getStatusCode().value(), e.getResponseBodyAsString());
                } catch (WebClientRequestException e) {
                        throw new CognitoException(503, "Something went wrong while connecting: " + e.getMessage());
                }
        }

        


         public SuccefullLoginResponse login(LoginRequest request, UserHeaderModel headers,String accessToken) {
               
                String xTrace = GenerateCookie.generateRequestId();// for all kind of the requestid we will use it

                String sessionToken = GenerateCookie.generateSessionToken(); 

                try {
                        return webClient.post()
                                        .uri(appCahe.getApiUrl(ApiEnums.LOGIN))
                                         .headers(httpHeaders -> {
                                                httpHeaders.set("Content-Type", "application/json; charset=UTF-8");
                                                httpHeaders.set("x-device-manufacturer", headers.getDeviceManufacturer());
                                                httpHeaders.set("x-app-version-code", headers.getAppVersionCode());
                                                httpHeaders.set("x-supply-apps-kit-version", headers.getSupplyAppsKitVersion());
                                                httpHeaders.set("version_name", headers.getVersionName());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-device-id", headers.getXDeviceId());
                                                httpHeaders.set("version_code", headers.getVersionCode());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-client-name", headers.getClientName());
                                                httpHeaders.set("model", headers.getModel());
                                                httpHeaders.set("x-device-hardware-type", headers.getDeviceHardwareType());
                                                httpHeaders.set("x-app-version", headers.getAppVersion());
                                                httpHeaders.set("version", headers.getVersion());
                                                httpHeaders.set("x-app-theme", headers.getAppTheme());
                                                httpHeaders.set("x-app-appearance", headers.getAppAppearance());
                                                httpHeaders.set("user-agent", safe(headers.getUserAgent()));
                                                httpHeaders.set("x-system-appearance", safe(headers.getAppAppearance()));
                                                httpHeaders.set("x-system-theme", safe(headers.getAppTheme()));
                                                httpHeaders.set("x-lat", safe(headers.getXLat()));
                                                httpHeaders.set("x-long", safe(headers.getXLong()));
                                                httpHeaders.set("accept", "application/json");
                                                httpHeaders.set("x-api-key", "b30153b3-a5f8-4118-9af9-43d05487c1b3");
                                                httpHeaders.set("x-grace-trace-id", xTrace);
                                                httpHeaders.set("requestid", xTrace);
                                                httpHeaders.set("x-request-id", xTrace);
                                                httpHeaders.set("session-token",sessionToken);
                                                httpHeaders.set("cookie", GenerateCookie.generateCfBmCookie());
                                                httpHeaders.set("x-app-locale", safe(headers.getAppLocale()));
                                                httpHeaders.set("access_token", accessToken);
                                                httpHeaders.set("priority", safe(headers.getPriority()));
                                        })
                                        .bodyValue(request)
                                        .retrieve()
                                        .bodyToMono(SuccefullLoginResponse.class)
                                        .block();
                } catch (WebClientResponseException e) {
                        throw new CognitoException(e.getStatusCode().value(), e.getResponseBodyAsString());
                } catch (WebClientRequestException e) {
                        throw new CognitoException(503, "Unable to connect to your storeops" + e.getMessage());
                }
        }


      public CognitoRefreshTokenRespons refreshToken(MultiValueMap<String,String> formData , UserHeaderModel headers) {
                String xTrace = GenerateCookie.generateRequestId();
                return webClient.post()
                                .uri(appCahe.getApiUrl(ApiEnums.ROATATE_TOKEN))
                                .headers(httpHeaders -> {
                                    httpHeaders.set("Content-Type", "application/x-www-form-urlencoded");
                                    httpHeaders.set("x-api-key", "b30153b3-a5f8-4118-9af9-43d05487c1b3");
                                    httpHeaders.set("requestId", xTrace);
                                    httpHeaders.set("X-Request-Id", xTrace);
                                    httpHeaders.set("x-gr-trace-id", xTrace);
                                    httpHeaders.set("x-app-locale", safe(headers.getAppLocale()));
                                    httpHeaders.set("X-Lat", safe(headers.getXLat()));
                                    httpHeaders.set("X-Long", safe(headers.getXLong()));
                                })
                                .body(BodyInserters.fromFormData(formData))
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

        public Mono<ResponseEntity<FetchSlotsResponse>> getSlotsDetails(UserHeaderModel headers,
                        FetchSlotsRequest fetchSlotsRequest) {

                String xTrace = GenerateCookie.generateRequestId(); // for all kind of the requestid we will use it

                

                return blinkClient.post()
                                .uri(appCahe.getApiUrl(ApiEnums.FETCH_SLOTS))
                                    .headers(httpHeaders -> {
                                                httpHeaders.set("Content-Type", "application/json; charset=UTF-8");
                                                httpHeaders.set("x-device-manufacturer", headers.getDeviceManufacturer());
                                                httpHeaders.set("x-app-version-code", headers.getAppVersionCode());
                                                httpHeaders.set("x-supply-apps-kit-version", headers.getSupplyAppsKitVersion());
                                                httpHeaders.set("version_name", headers.getVersionName());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-device-id", headers.getXDeviceId());
                                                httpHeaders.set("version_code", headers.getVersionCode());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-client-name", headers.getClientName());
                                                httpHeaders.set("model", headers.getModel());
                                                httpHeaders.set("x-device-hardware-type", headers.getDeviceHardwareType());
                                                httpHeaders.set("x-app-version", headers.getAppVersion());
                                                httpHeaders.set("version", headers.getVersion());
                                                httpHeaders.set("x-app-theme", headers.getAppTheme());
                                                httpHeaders.set("x-app-appearance", headers.getAppAppearance());
                                                httpHeaders.set("user-agent", safe(headers.getUserAgent()));
                                                httpHeaders.set("x-system-appearance", safe(headers.getAppAppearance()));
                                                httpHeaders.set("x-system-theme", safe(headers.getAppTheme()));
                                                httpHeaders.set("x-lat", safe(headers.getXLat()));
                                                httpHeaders.set("x-long", safe(headers.getXLong()));
                                                httpHeaders.set("accept", "application/json");
                                                httpHeaders.set("x-api-key", "b30153b3-a5f8-4118-9af9-43d05487c1b3");
                                                httpHeaders.set("x-grace-trace-id", xTrace);
                                                httpHeaders.set("requestid", xTrace);
                                                httpHeaders.set("x-request-id", xTrace);
                                                httpHeaders.set("session-token", GenerateCookie.generateSessionToken());
                                                httpHeaders.set("cookie", GenerateCookie.generateCfBmCookie());
                                                httpHeaders.set("x-app-locale", safe(headers.getAppLocale()));
                                                httpHeaders.set("access_token", headers.getAccessToken());
                                                httpHeaders.set("priority", safe(headers.getPriority()));
                                        })
                                .bodyValue(fetchSlotsRequest)
                                .retrieve()
                                .toEntity(FetchSlotsResponse.class);
        }

        public Mono<ResponseEntity<GlobalRespons>> bookSlots(UserHeaderModel headers,
                        BookSlotsRequest request,String timesLog) {

                String xTrace = GenerateCookie.generateRequestId();

                return blinkClient.post()
                                .uri(appCahe.getApiUrl(ApiEnums.BOOK_SLOT))
                                .headers(httpHeaders -> {
                                                httpHeaders.set("Content-Type", "application/json; charset=UTF-8");
                                                httpHeaders.set("x-device-manufacturer", headers.getDeviceManufacturer());
                                                httpHeaders.set("x-app-version-code", headers.getAppVersionCode());
                                                httpHeaders.set("x-supply-apps-kit-version", headers.getSupplyAppsKitVersion());
                                                httpHeaders.set("version_name", headers.getVersionName());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-device-id", headers.getXDeviceId());
                                                httpHeaders.set("version_code", headers.getVersionCode());
                                                httpHeaders.set("app_client", headers.getAppClient());
                                                httpHeaders.set("x-client-name", headers.getClientName());
                                                httpHeaders.set("model", headers.getModel());
                                                httpHeaders.set("x-device-hardware-type", headers.getDeviceHardwareType());
                                                httpHeaders.set("x-app-version", headers.getAppVersion());
                                                httpHeaders.set("version", headers.getVersion());
                                                httpHeaders.set("x-app-theme", headers.getAppTheme());
                                                httpHeaders.set("x-app-appearance", headers.getAppAppearance());
                                                httpHeaders.set("user-agent", safe(headers.getUserAgent()));
                                                httpHeaders.set("x-system-appearance", safe(headers.getAppAppearance()));
                                                httpHeaders.set("x-system-theme", safe(headers.getAppTheme()));
                                                httpHeaders.set("x-lat", safe(headers.getXLat()));
                                                httpHeaders.set("x-long", safe(headers.getXLong()));
                                                httpHeaders.set("accept", "application/json");
                                                httpHeaders.set("x-api-key", "b30153b3-a5f8-4118-9af9-43d05487c1b3");
                                                httpHeaders.set("x-grace-trace-id", xTrace);
                                                httpHeaders.set("requestid", xTrace);
                                                httpHeaders.set("x-request-id", xTrace);
                                                httpHeaders.set("session-token", GenerateCookie.generateSessionToken());
                                                httpHeaders.set("cookie", GenerateCookie.generateCfBmCookie());
                                                httpHeaders.set("x-app-locale", safe(headers.getAppLocale()));
                                                httpHeaders.set("access_token", headers.getAccessToken());
                                                httpHeaders.set("priority", safe(headers.getPriority()));
                                        })
                                .bodyValue(request)
                                .retrieve()
                                .toEntity(GlobalRespons.class);
                                
        }

        public Mono<AvailableSlotsRespons> getAvailableSlots(String cfBm, String requestId, String httpSessionToken,
                        String sessionToken, String siteId, UserModel user, ViewAvailaibleSlotsRequest viewAvailableSlotsRequest,
                        String jwt, String userAgent) {
                
                String authHeader = jwt != null && jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;

                return blinkClient.post()
                                .uri(appCahe.getApiUrl(ApiEnums.FETCH_SLOTS))
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

}
