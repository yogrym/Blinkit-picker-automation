package com.picker.BlinkitPicker.Services;

import java.util.Optional;

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

@Service
public class AuthServices {

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

        userOpt.get().setJwt(verifyRespons.getAuthenticationResult().getIdToken());
        userOpt.get().setRefreshToken(verifyRespons.getAuthenticationResult().getRefreshToken());

        UserHeaderModel headers = userOpt.get().getUserHeaders();

        if (headers == null) {
            headers = new UserHeaderModel();
            userOpt.get().setUserHeaders(headers);
        }
        headers.setAuthorization("Bearer " + verifyRespons.getAuthenticationResult().getIdToken());
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

        user.get().setJwt(respons.getAuthenticationResult().getIdToken());
        userRepo.save(user.get());

        return true;
    }
}
