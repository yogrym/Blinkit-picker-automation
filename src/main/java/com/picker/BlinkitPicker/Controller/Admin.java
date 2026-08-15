package com.picker.BlinkitPicker.Controller;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.picker.BlinkitPicker.Dto.request.SignupRequest;
import com.picker.BlinkitPicker.Dto.respons.AdminUserListResponse;
import com.picker.BlinkitPicker.Dto.respons.SignupRespons;
import com.picker.BlinkitPicker.Services.AdminServices;

@RestController
@RequestMapping("/admin")
public class Admin {
    @Autowired
    private AdminServices adminServices;

    @PostMapping("/add-user")
    public ResponseEntity<SignupRespons> addUser(@RequestBody SignupRequest request) {
        SignupRespons response = adminServices.addUser(request);
        if ("failure".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-users")
    public ResponseEntity<AdminUserListResponse> getUsers(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(adminServices.getUsers(page, size));
    }

    @GetMapping("/search-user")
    public ResponseEntity<AdminUserListResponse> searchUserByPhone(@RequestParam("phone") String phone) {
        return ResponseEntity.ok(adminServices.searchUserByPhone(phone));
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<?> deleteUser(@RequestParam("userId") Long userId) {
        try {
            return ResponseEntity.ok(adminServices.deleteUser(userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/block-user")
    public ResponseEntity<?> blockUser(@RequestParam("userId") Long userId) {
        try {
            return ResponseEntity.ok(adminServices.blockUser(userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/unblock-user")
    public ResponseEntity<?> unblockUser(@RequestParam("userId") Long userId) {
        try {
            return ResponseEntity.ok(adminServices.unblockUser(userId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/change-role")
    public ResponseEntity<?> changeRole(@RequestBody java.util.Map<String, String> body) {
        try {
            Long userId = Long.valueOf(body.get("userId"));
            String role = body.get("role");
            return ResponseEntity.ok(adminServices.changeRole(userId, role));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PutMapping("/change-store")
    public ResponseEntity<?> changeStoreId(
            @RequestHeader("Authorization") String token,
            @RequestBody java.util.Map<String, String> body) {
        try {
            Long userId = Long.valueOf(body.get("userId"));
            String newStoreId = body.get("storeId");
            return ResponseEntity.ok(adminServices.changeStoreIdByAdminOrMaintainer(token, userId, newStoreId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/stop-session")
    public ResponseEntity<?> stopSession(
            @RequestHeader("Authorization") String token,
            @RequestParam("userId") Long userId,
            @RequestParam("sessionId") String sessionId) {
        try {
            return ResponseEntity.ok(adminServices.stopUserSession(token, userId, sessionId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/pause-session")
    public ResponseEntity<?> pauseSession(
            @RequestHeader("Authorization") String token,
            @RequestParam("userId") Long userId,
            @RequestParam("sessionId") String sessionId) {
        try {
            return ResponseEntity.ok(adminServices.pauseUserSession(token, userId, sessionId));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }

    @PostMapping("/renew-plan")
    public ResponseEntity<?> renewPlan(
            @RequestHeader("Authorization") String token,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String planType = body.get("plan").toString();
            return ResponseEntity.ok(adminServices.renewPlan(token, userId, planType));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
