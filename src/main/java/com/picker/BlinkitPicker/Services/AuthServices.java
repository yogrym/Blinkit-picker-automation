package com.picker.BlinkitPicker.Services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Dto.LoginRequest;
import com.picker.BlinkitPicker.Dto.LoginRespons;
import com.picker.BlinkitPicker.Dto.OtpAuthRespons;
import com.picker.BlinkitPicker.Dto.OtpValidRespons;
import com.picker.BlinkitPicker.Dto.VerifyOtpClientRequest;
import com.picker.BlinkitPicker.Dto.VerifyOtpRespons;
import com.picker.BlinkitPicker.Dto.RefreshTokenRequest;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
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

    public ResponseEntity<?> loginWithOtp(LoginRequest loginRequest) {

        Optional<UserModel> userOpt = userRepo.findByPhone(loginRequest.getPhoneNumber());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid phone number");
        }

        OtpAuthRespons respons = webClientServices.sendOtpToUser(userOpt.get().getPhone());
        if (respons.getChallengeName() == null) {
            return ResponseEntity.status(500).body("Something went wrong while sending OTP.");
        }

        return ResponseEntity.status(200).body(OtpValidRespons.builder()
                .username(respons.getChallengeParameters().getUsername())
                .session(respons.getSession())
                .userId(userOpt.get().getId().toString())
                .build());

    }

    public ResponseEntity<?> verifyLogin(VerifyOtpClientRequest request) {

        Optional<UserModel> userOpt = userRepo.findById(Long.parseLong(request.getUserId()));

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(400).body("");
        }

        VerifyOtpRespons verifyRespons = webClientServices.verifyOtp(
                request.getUserName(),
                request.getAnswer(),
                request.getSession(),
                userOpt.get().getPhone());

        if (verifyRespons.getAuthenticationResult() == null) {
            return ResponseEntity.status(500).body("Something went wrong while verifying OTP.");
        }

        String idToken = verifyRespons.getAuthenticationResult().getIdToken();

        userOpt.get().setJwt(idToken);
        userOpt.get().setRefreshToken(verifyRespons.getAuthenticationResult().getRefreshToken());

        UserHeaderModel headers = userOpt.get().getUserHeaders();

        if (headers == null) {
            headers = new UserHeaderModel();
            userOpt.get().setUserHeaders(headers);
        }
        headers.setAuthorization("Bearer " + idToken);
        headers.setXLat(request.getXLat());
        headers.setXLong(request.getXLong());

        try {
            JsonNode idTokenClaims = extractJwtPayload(idToken);
            headers.setEmployeeId(getTextClaim(idTokenClaims, "employeeId"));
            headers.setEmployeeName(getTextClaim(idTokenClaims, "employeeName"));
            headers.setSiteId(getTextClaim(idTokenClaims, "siteId"));
            headers.setXDeviceId(UUID.randomUUID().toString() + ":" + System.currentTimeMillis());
            headers.setRole(extractPickerRole(getTextClaim(idTokenClaims, "roles")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("try again later");
        }

        userRepo.save(userOpt.get());

        String accessToken = jwtServices.generateAccessToken(userOpt.get());
        String refreshToken = jwtServices.generateRefreshToken(userOpt.get());

        return ResponseEntity.status(200).body(LoginRespons.builder()
                .token(accessToken)
                .refreshtoken(refreshToken)
                .message("success")
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

        return ResponseEntity.status(200).body(LoginRespons.builder()
                .token(newAccessToken)
                .refreshtoken(newRefreshToken)
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
