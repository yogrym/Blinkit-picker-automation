package com.picker.BlinkitPicker.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Model.BookingTaskModel;
import com.picker.BlinkitPicker.Repository.BookingTaskRepo;
import com.picker.BlinkitPicker.Services.Worker.BookingWorker;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchedulerServices {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServices.class);

    @Autowired
    private BookingServices bookingServices;

    @Autowired
    private BookingTaskRepo bookingTaskRepo;

  
    @Scheduled(fixedRate = 600000)
    public void proactivelyRefreshTokens() {
        logger.info("[Scheduler] Checking for tokens that need proactive refreshing...");
        
        ConcurrentHashMap<String, WorkerList> workerMap = bookingServices.getWorkerMap();
        List<BookingTaskModel> activeTasks = bookingTaskRepo.findByActiveTrue();
        
        for (BookingTaskModel task : activeTasks) {
           
                
                String userId = task.getUserId().toString();
                String sessionId = task.getSessionId();
                
                WorkerList workerList = workerMap.get(userId);
                if (workerList != null) {
                    BookingWorker worker = workerList.getWorker(sessionId);
                    if (worker != null) {
                        try {
                            logger.info("[Scheduler] Proactively refreshing token for user {} session {}", userId, sessionId);
                            boolean success = worker.forceAccessTokenRefresh();
                            if (success) {
                               
                                task.setLastTokenRefreshed(LocalDateTime.now());
                                bookingTaskRepo.save(task);
                                logger.info("[Scheduler] Successfully refreshed token and updated DB for session {}", sessionId);
                            }
                        } catch (Exception e) {
                            logger.error("[Scheduler] Error refreshing token for session {}: {}", sessionId, e.getMessage());
                        }
                    }
                }
            
        }
    }
}
