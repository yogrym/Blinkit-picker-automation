package com.picker.BlinkitPicker.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Dto.request.BookingRequest;
import com.picker.BlinkitPicker.Dto.respons.LogsResponse;
import com.picker.BlinkitPicker.Services.BookingServices;

@RestController
@RequestMapping("/task")
public class MainController {

    
    private final BookingServices bookingServices;

    public MainController(BookingServices bookingServices) {
        this.bookingServices = bookingServices;
    }

    @PostMapping("/booking")
    public ResponseEntity<?> startBooking(@RequestHeader("Authorization") String token,
            @RequestBody BookingRequest request) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        WorkerList.BookingData bookingData = bookingServices.startBooking(token, request);
        return ResponseEntity.ok(bookingData);
    }

    @GetMapping("/booking-data")
    public ResponseEntity<?> getBookingData(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            return ResponseEntity.status(401).body("Invalid token");
        }

        return ResponseEntity.ok(bookingServices.getBookingData(token));
    }

    @PostMapping("/stop/{sessionId}")
    public ResponseEntity<?> stopBooking(@RequestHeader("Authorization") String token,
            @PathVariable String sessionId) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            return ResponseEntity.status(401).body("Invalid token");
        }
        return ResponseEntity.ok(bookingServices.stopBooking(token, sessionId));
    }

    @PostMapping("/pause/{sessionId}")
    public ResponseEntity<?> pauseBooking(@RequestHeader("Authorization") String token,
            @PathVariable String sessionId) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            return ResponseEntity.status(401).body("Invalid token");
        }
        return ResponseEntity.ok(bookingServices.pauseBooking(token, sessionId));
    }

    @PostMapping("/resume/{sessionId}")
    public ResponseEntity<?> resumeBooking(@RequestHeader("Authorization") String token,
            @PathVariable String sessionId) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            return ResponseEntity.status(401).body("Invalid token");
        }
        return ResponseEntity.ok(bookingServices.resumeBooking(token, sessionId));
    }

    @PostMapping("/change-store")
    public ResponseEntity<?> changeStoreId(@RequestHeader("Authorization") String token,
            @RequestBody java.util.Map<String, String> body) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            return ResponseEntity.status(401).body("Invalid token");
        }

        String storeId = body.get("store_id");
        if (storeId == null || storeId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("storeId is required in the body");
        }

        try {
            return ResponseEntity.ok(bookingServices.changeStoreId(token, storeId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/logs/{sessionId}")
    public ResponseEntity<?> getSessionLogs(@RequestHeader("Authorization") String token,
            @PathVariable String sessionId,
            @RequestParam(required = false, defaultValue = "-1") int afterIndex) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            return ResponseEntity.status(401).body("Invalid token");
        }

        LogsResponse logsResponse = bookingServices.getSessionLogs(token, sessionId, afterIndex);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Logs-Reset", String.valueOf(logsResponse.isReset()));
        headers.add("Access-Control-Expose-Headers", "X-Logs-Reset");

        return ResponseEntity.ok().headers(headers).body(logsResponse.getLogs());
    }

    @DeleteMapping("remove/{date}/{sessionId}")
    public ResponseEntity<?> removeDateAndTimeFromBookingSession(@RequestHeader("Authorization") String token,
            @PathVariable String date,
            @RequestParam("time") String time,
            @PathVariable String sessionId) {

        try {
            return ResponseEntity.ok(bookingServices.removeDate(date, time, token, sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("get-session-data/{sessionID}")
    public ResponseEntity<?> getSessionTimeAndDate(@PathVariable String sessionID,
            @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(bookingServices.getSessionTimeAndDate(token, sessionID));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }

    }

    @GetMapping("get-slots")
    public ResponseEntity<?> getSlots(@RequestHeader("Authorization") String token) {

        try {
            return ResponseEntity.ok(bookingServices.getAvailableSlots(token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
