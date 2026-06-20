package com.picker.BlinkitPicker.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.picker.BlinkitPicker.Dto.LoginRequest;
import com.picker.BlinkitPicker.Dto.LoginRespons;
import com.picker.BlinkitPicker.Services.AuthServices;

@RestController
@RequestMapping("/auth")
public class Auth {
    @Autowired
    private AuthServices authService;

    @PostMapping("/login")
    public ResponseEntity<LoginRespons> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

}
