package com.picker.BlinkitPicker.Services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.LoginRequest;
import com.picker.BlinkitPicker.Dto.LoginRespons;
import com.picker.BlinkitPicker.Dto.OtpAuthRespons;
import com.picker.BlinkitPicker.Dto.OtpValidRespons;
import com.picker.BlinkitPicker.Dto.VerifyOtpClientRequest;
import com.picker.BlinkitPicker.Dto.VerifyOtpRespons;
import com.picker.BlinkitPicker.Model.UserModel;
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

        Optional<UserModel> userOpt = userRepo.findByApiKey(loginRequest.getKey());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid API Key.");
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
        userOpt.get().getUserHeaders()
                .setAuthorization("Bearer " + verifyRespons.getAuthenticationResult().getIdToken());
        userRepo.save(userOpt.get());

        String accessToken = jwtServices.generateAccessToken(userOpt.get());
        String refreshToken = jwtServices.generateRefreshToken(userOpt.get());

        return ResponseEntity.status(200).body(LoginRespons.builder()
                .token(accessToken)
                .refreshtoken(refreshToken)
                .message("success")
                .build());
    }
}
