package com.picker.BlinkitPicker.Services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.Internal_Request.AuthHeader;
import com.picker.BlinkitPicker.Dto.Internal_Respons.LoginRespons;
import com.picker.BlinkitPicker.Dto.request.LoginRequest;
import com.picker.BlinkitPicker.Dto.request.RefreshTokenRequest;
import com.picker.BlinkitPicker.Dto.request.VerifyOtpRequest;
import com.picker.BlinkitPicker.Dto.respons.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Dto.respons.AppLoginRespons;
import com.picker.BlinkitPicker.Dto.respons.OtpAuthRespons;
import com.picker.BlinkitPicker.Dto.respons.OtpValidRespons;
import com.picker.BlinkitPicker.Dto.respons.VerifyOtpRespons;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.ApiKeyGenerator;
import com.picker.BlinkitPicker.Util.GenerateCookie;
import com.picker.BlinkitPicker.Util.SessionIdGenerator;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuthServices {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserRepo userRepo;
    private final JwtServices jwtServices;

   

    @Autowired
    private WebClientServices webClientServices;

    public AuthServices(UserRepo userRepo, JwtServices jwtServices) {
        this.userRepo = userRepo;
        this.jwtServices = jwtServices;
        
    }

    public ResponseEntity<?> sendOtp(LoginRequest loginRequest) {

        Optional<UserModel> userOpt = userRepo.findByPhone(loginRequest.getPhoneNumber());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid phone number");
        }

        String requestId = GenerateCookie.generateRequestId();
        
       AuthHeader authHeader = AuthHeader.builder()
                .grTraceId(requestId)
                .cookie(GenerateCookie.generateCfBmCookie())
                .lattitude(loginRequest.getXLat())
                .longitude(loginRequest.getXLong())
                .requestId(requestId)
                .requestIdLower(requestId)
                .build();
             
        OtpAuthRespons respons = webClientServices.sendOtpToUser(userOpt.get().getPhone(),authHeader);
        if (respons.isSmsSent()== false ) {
            return ResponseEntity.status(500).body(respons.getMessage());
        }

        return ResponseEntity.status(200).body(respons.getMessage());

    }

    public ResponseEntity<?> verifyCode(VerifyOtpRequest request) {
       
        UserModel userOpt = userRepo.findByPhone(request.getUserNumber()).orElseThrow(() -> new RuntimeException("Invalid Phone Number"));

        String requestId = GenerateCookie.generateRequestId();

         AuthHeader authHeader = AuthHeader.builder()
                .grTraceId(requestId)
                .cookie(GenerateCookie.generateCfBmCookie())
                .lattitude(request.getXLat())
                .longitude(request.getXLong())
                .requestId(requestId)
                .requestIdLower(requestId)
                .build();

        VerifyOtpRespons verifyRespons = webClientServices.verifyOtp(request,authHeader);

        if (verifyRespons.isSuccess() == false && verifyRespons.getAccessToken() == null) {
            return ResponseEntity.status(500).body("Something went wrong while verifying OTP.");
        }

        String accessToken = verifyRespons.getAccessToken();
        String refreshToken = verifyRespons.getRefreshToken();

        userOpt.getUserHeaders().setAccessToken(accessToken);
        userOpt.getUserHeaders().setRefreshToken(refreshToken);
        

        UserHeaderModel headers = userOpt.getUserHeaders();
        if (userOpt.getApiKey() == null) {
            userOpt.setApiKey(ApiKeyGenerator.generateApiKey());
        }

       
        headers = UserHeaderModel.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .employeeId(verifyRespons.getUser().getId())
                .phone(verifyRespons.getUser().getPhone())
                .xLat(request.getXLat())
                .xLong(request.getXLong())
                .build();

        userOpt.setUserHeaders(headers);

        String appAccessToken = jwtServices.generateAccessToken(userOpt);
        String appRefreshToken = jwtServices.generateRefreshToken(userOpt);

        String requestId2 = GenerateCookie.generateRequestId();

        AuthHeader headersForLogin = AuthHeader.builder()
                .grTraceId(requestId2)
                .cookie(GenerateCookie.generateCfBmCookie())
                .accessToken(verifyRespons.getAccessToken())
                .lattitude(request.getXLat())
                .longitude(request.getXLong())
                .requestId(requestId2)
                .requestIdLower(requestId2)
                .build();

        LoginRespons successLoginRespons = webClientServices.loginUser(headersForLogin, request);

        if(successLoginRespons != null && successLoginRespons.getUserData() != null) {
            userOpt.getUserHeaders().setAccessToken(accessToken);
            userOpt.getUserHeaders().setRefreshToken(refreshToken);
            userOpt.getUserHeaders().setEmployeeName(successLoginRespons.getUserData().getName());
            userOpt.getUserHeaders().setEmployeeId(successLoginRespons.getUserData().getRoleDetails().getEmployeeId());
            userOpt.getUserHeaders().setSiteName(successLoginRespons.getUserData().getSiteName());
            userRepo.save(userOpt);
        } else {
            return ResponseEntity.status(500).body("Something went wrong while logging in.");
        }

        return ResponseEntity.status(200).body(AppLoginRespons.builder()
                .success(verifyRespons.isSuccess())
                .verified(verifyRespons.isVerified())
                .message("Phone number verified successfully")
                .aurtorization(AppLoginRespons.ApplicationAuthorization.builder()
                        .appAccessToken(appAccessToken)
                        .appRefreshToken(appRefreshToken) // aplication access token of our application aurthorizatrion
                    .build())
                .build());
    }

    public ResponseEntity<?> refreshAccessToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (jwtServices.isTokenExpired(token)) {
            return ResponseEntity.status(401).body("Refresh token has expired");
        }

        Long userId;
        try {
            userId = jwtServices.extractUserId(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }

        Optional<UserModel> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }

        UserModel user = userOpt.get();
        String newAccessToken = jwtServices.generateAccessToken(user);
        String newRefreshToken = jwtServices.generateRefreshToken(user);

        return ResponseEntity.status(200).body(AppLoginRespons.builder()
                .aurtorization(AppLoginRespons.ApplicationAuthorization.builder()
                        .appAccessToken(newAccessToken)
                        .appRefreshToken(newRefreshToken)
                        .build())
                .message("success")
                .build());
    }

    public Boolean generateInternalToken(String token) {

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = jwtServices.extractUserId(token);

        if (userId == null) {
            return false;
        }

        Optional<UserModel> user = userRepo.findById(userId);
        if (user.isEmpty()) {
            return false;
        }

        CognitoRefreshTokenRespons respons = webClientServices.refreshToken(user.get().getRefreshToken());
        if (respons.getAuthenticationResult() == null) {
            return null;
        }

        String freshToken = respons.getAuthenticationResult().getIdToken();
        user.get().setJwt(freshToken);
        if (user.get().getUserHeaders() != null) {
            user.get().getUserHeaders().setAuthorization(freshToken);
        }
        userRepo.save(user.get());

        return true;
    }

    private JsonNode extractJwtPayload(String token) throws Exception {
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT token");
        }

        byte[] decodedPayload = Base64.getUrlDecoder().decode(tokenParts[1]);
        return OBJECT_MAPPER.readTree(new String(decodedPayload, StandardCharsets.UTF_8));
    }

    private String getTextClaim(JsonNode claims, String claimName) {
        JsonNode claim = claims.get(claimName);
        if (claim == null || claim.isNull()) {
            return null;
        }
        return claim.asString();
    }

    private String extractPickerRole(String rolesJson) throws Exception {
        if (rolesJson == null || rolesJson.isBlank()) {
            return null;
        }

        JsonNode allowedRoles = OBJECT_MAPPER.readTree(rolesJson)
                .path("STOREOPS")
                .path("allowed_roles");

        if (allowedRoles.isArray()) {
            for (JsonNode allowedRole : allowedRoles) {
                if ("PICKER".equalsIgnoreCase(allowedRole.asString())) {
                    return "PICKER";
                }
            }
            return null;
        }

        String role = allowedRoles.asString();
        if (role == null) {
            return null;
        }

        for (String allowedRole : role.split(",")) {
            if ("PICKER".equalsIgnoreCase(allowedRole.trim())) {
                return "PICKER";
            }
        }

        return null;
    }
}
