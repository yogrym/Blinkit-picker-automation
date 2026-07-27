package com.picker.BlinkitPicker.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.picker.BlinkitPicker.Dto.request.LoginRequest;
import com.picker.BlinkitPicker.Dto.request.RefreshTokenRequest;
import com.picker.BlinkitPicker.Dto.request.VerifyOtpRequest;
import com.picker.BlinkitPicker.Services.AuthServices;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class Auth {
    @Autowired
    private AuthServices authService;

    @PostMapping("/send_otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.sendOtp(loginRequest));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyCode(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return authService.refreshAccessToken(refreshTokenRequest);
    }

    @PostMapping("/internal-token")
    public ResponseEntity<?> internalToken(@RequestHeader("Authorization") String token) {

        Boolean isRefreshed = authService.generateInternalToken(token);

        if (isRefreshed == false) {
            return ResponseEntity.status(401).body("Failed to refresh token");
        }

        return ResponseEntity.ok("success");
    }
}
