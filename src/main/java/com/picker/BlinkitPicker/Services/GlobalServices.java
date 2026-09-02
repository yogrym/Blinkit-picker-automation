package com.picker.BlinkitPicker.Services;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.SlotInformation;
import com.picker.BlinkitPicker.Dto.UserDetails;
import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Dto.request.CheckAvailableSlotsRequest;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;

import io.jsonwebtoken.Claims;

@Service
public class GlobalServices {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private WebClientServices webClientServices;

    @Autowired
    @Lazy
    private BookingServices bookingServices;

    public ResponseEntity<UserDetails> getUserDetails(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || jwtServices.isTokenExpired(token)) {
            return ResponseEntity.status(403).build();
        }

        Claims claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            return ResponseEntity.status(403).build();
        }

        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            return ResponseEntity.status(403).build();
        }

        Optional<UserModel> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(403).build();
        }

        UserModel user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        UserHeaderModel headers = user.getUserHeaders();
        String storeId = headers != null ? headers.getSiteId() : null;
        String employeeName = headers != null ? headers.getEmployeeName() : null;
        String employeeId = headers != null ? headers.getEmployeeId() : null;

        String remainingPlanValidity = "0 days 00:00:00";
        LocalDateTime expiresAt = user.getExpiresAt();
        if (expiresAt != null && expiresAt.isAfter(now)) {
            java.time.Duration duration = java.time.Duration.between(now, expiresAt);
            long secondsTotal = duration.getSeconds();
            long days = secondsTotal / 86400;
            long hours = (secondsTotal % 86400) / 3600;
            long minutes = (secondsTotal % 3600) / 60;
            long seconds = secondsTotal % 60;
            remainingPlanValidity = String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds);
        }

        boolean isExpired = (user.getExpired() != null && user.getExpired())
                || (expiresAt != null && expiresAt.isBefore(now));

        List<WorkerList.BookingData> bookingSessions = Collections.emptyList();

        ConcurrentHashMap<String, WorkerList> workerMap = bookingServices.getWorkerMap();
        if (workerMap != null && workerMap.containsKey(userId.toString())) {
            WorkerList workerList = workerMap.get(userId.toString());
            if (workerList != null) {
                bookingSessions = workerList.getAllBookingData();
            }
        }

        SlotInformation slotInformation = webClientServices.getAvailableSlots(CheckAvailableSlotsRequest.builder()
                .locationInfo(CheckAvailableSlotsRequest.LocationInfo.builder()
                        .latitude(Double.valueOf(headers.getXLat()))
                        .longitude(Double.valueOf(headers.getXLong()))
                        .placeId("")
                        .placeName("")
                        .build())
                .build()
        , headers, headers.getAccessToken());

        UserDetails.UserData userData = UserDetails.UserData.builder()
                .storeId(storeId)
                .employeeName(employeeName)
                .employeeId(employeeId)
                .totalBookedSlots(user.getTotalBookedSlots() != null ? user.getTotalBookedSlots().intValue() : 0)
                .remainingPlanValidity(remainingPlanValidity)
                .phoneNumber(user.getPhone())
                .blocked(user.getBlocked())
                .expiresAt(expiresAt)
                .role(user.getRole())
                .isExpired(isExpired)
                .bookingSessions(bookingSessions)
                .build();

       

        UserDetails userDetails = UserDetails.builder()
                .info(userData)
                .slotInformation(slotInformation)
                .build();

        return ResponseEntity.ok(userDetails);
    }
}
