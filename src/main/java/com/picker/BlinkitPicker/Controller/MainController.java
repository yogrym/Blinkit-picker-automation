package com.picker.BlinkitPicker.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.picker.BlinkitPicker.Dto.BookingRequest;
import com.picker.BlinkitPicker.Services.BookingServices;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/task")
public class MainController {

    @Autowired
    private BookingServices bookingServices;

    @PostMapping("/booking")
    public ResponseEntity<?> startBooking(@RequestHeader("Authorization") String token,
            @RequestBody BookingRequest request) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        bookingServices.startBooking(token, request);
        return ResponseEntity.ok("Booking started");
    }
}
