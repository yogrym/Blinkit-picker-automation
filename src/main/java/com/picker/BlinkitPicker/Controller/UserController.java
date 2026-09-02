package com.picker.BlinkitPicker.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.picker.BlinkitPicker.Services.GlobalServices;

@RestController
public class UserController {

    @Autowired
    private GlobalServices globalServices;

    @GetMapping("/user-details")
    public ResponseEntity<?> getUserDetails(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return ResponseEntity.status(403).body("");
        }
        return globalServices.getUserDetails(authorizationHeader);
    }

   /*  @GetMapping("/user-details/slot-information")
    public ResponseEntity<?> getSlotInformation(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return ResponseEntity.status(403).body("");
        }
        return globalServices.getSlotInformation(authorizationHeader);
    } */
   


    
}
