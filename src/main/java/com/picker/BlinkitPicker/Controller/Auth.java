package com.picker.BlinkitPicker.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.picker.BlinkitPicker.Dto.LoginRequest;
import com.picker.BlinkitPicker.Dto.LoginRespons;
import com.picker.BlinkitPicker.Dto.VerifyOtpClientRequest;
import com.picker.BlinkitPicker.Services.AuthServices;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class Auth {
    @Autowired
    private AuthServices authService;

    @PostMapping("/login")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.loginWithOtp(loginRequest));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpClientRequest verifyOtpClientRequest) {
        return ResponseEntity.ok(authService.verifyLogin(verifyOtpClientRequest));
    }

}
