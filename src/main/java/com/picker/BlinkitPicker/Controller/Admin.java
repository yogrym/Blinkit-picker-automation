package com.picker.BlinkitPicker.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.picker.BlinkitPicker.Dto.SignupRequest;
import com.picker.BlinkitPicker.Dto.SignupRespons;
import com.picker.BlinkitPicker.Services.AdminServices;

@RestController
@RequestMapping("/admin")
public class Admin {
    @Autowired
    private AdminServices adminServices;

    @PostMapping("/add-user")
    public ResponseEntity<SignupRespons> addUser(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(adminServices.addUser(request));
    }
}
