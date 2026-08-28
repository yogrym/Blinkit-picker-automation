package com.picker.BlinkitPicker.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.picker.BlinkitPicker.Dto.request.BookingRequest;
import com.picker.BlinkitPicker.Dto.respons.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Model.BookingTaskModel;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.BookingTaskRepo;
import com.picker.BlinkitPicker.Repository.UserRepo;
import java.util.List;

@Service
public class SchedulerServices {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServices.class);

  
    private BookingServices bookingServices;

   
    private BookingTaskRepo bookingTaskRepo;

    private UserRepo userRepo;

    private WebClientServices webClientServices;


    public SchedulerServices (BookingServices bookingServices, BookingTaskRepo bookingTaskRepo,
        UserRepo userRepo,WebClientServices webclientServices) {
        this.bookingServices = bookingServices;
        this.bookingTaskRepo = bookingTaskRepo;
        this.userRepo = userRepo;
        this.webClientServices = webclientServices;
    }

  
    @Scheduled(fixedRate = 18000000) 
    public void proactivelyRefreshTokens() {
        logger.info("[Scheduler] Checking for tokens that need proactive refreshing...");
        
        try {
            List<BookingTaskModel> activeTasks = bookingTaskRepo.findByActiveTrue();
            java.util.Set<Long> refreshedUsers = new java.util.HashSet<>();
            
            for (BookingTaskModel task : activeTasks) {
                try {
                    if (task == null || task.getUserInfo() == null || task.getUserInfo().getUserModel() == null || task.getUserInfo().getUserModel().getId() == null || task.getSessionInfo() == null) {
                        logger.warn("[Scheduler] Found a task with missing required fields (e.g. null userId). Skipping.");
                        continue;
                    }

                    Long userId = task.getUserInfo().getUserModel().getId();
                    UserModel user = userRepo.findById(userId).orElse(null);

                    if (user != null) {
                        UserHeaderModel headers = user.getUserHeaders();
                        if (headers == null || headers.getRefreshToken() == null) {
                             logger.warn("[Scheduler] Missing headers or refresh token for user {}. Skipping.", userId);
                             continue;
                        }

                        if (!refreshedUsers.contains(userId)) {
                            MultiValueMap<String,String> formData = new LinkedMultiValueMap<>();
                            formData.add("refresh_token", headers.getRefreshToken());

                            CognitoRefreshTokenRespons response = webClientServices.refreshToken(formData, headers);

                            if (response == null || !response.getSuccess()) {
                                logger.warn("[Scheduler] Couldn't refresh token for user {} ", headers.getEmployeeName());
                                continue;
                            }
                            
                            headers.setRefreshToken(response.getRefreshToken());
                            headers.setAccessToken(response.getAccessToken());
                            user.setUserHeaders(headers);
                            userRepo.save(user);
                            refreshedUsers.add(userId);

                            logger.info("User {} has been updated with new tokens.", headers.getEmployeeName());
                        }

                        String sessionId = task.getSessionId();
                        
                        bookingServices.stopBookingFromSheduler(sessionId, userId);
                        bookingServices.startBookingFromSheduler(task, user);

                        logger.info("Restarted session {} for user {}", sessionId, headers.getEmployeeName());
                    }
                  
                } catch (Exception e) {
                    logger.error("[Scheduler] Error processing task for session {}: {}", task.getSessionId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("[Scheduler] Fatal error in proactivelyRefreshTokens scheduler: {}", e.getMessage(), e);
        }
    }
}
