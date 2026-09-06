package com.picker.BlinkitPicker.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SessionManagerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SessionManagerScheduler.class);

   
    private static final long SHUFFLE_MIN_MS = 10_000L; 
    private static final long SHUFFLE_MAX_MS = 20_000L; 

    private final BookingServices bookingServices;

    public SessionManagerScheduler(BookingServices bookingServices) {
        this.bookingServices = bookingServices;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void enableShuffleAtMidnight() {
        logger.info("[SessionManager] Midnight reached — enabling shuffle ({} s – {} s) for all workers.",
                SHUFFLE_MIN_MS / 1000, SHUFFLE_MAX_MS / 1000);
        bookingServices.applyShuffleToAllUsers(SHUFFLE_MIN_MS, SHUFFLE_MAX_MS);
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void disableShuffleAtFiveAm() {
        logger.info("[SessionManager] 05:00 AM reached — disabling shuffle, workers resuming normal poll intervals.");
        bookingServices.disableShuffleForAllUsers();
    }
}
