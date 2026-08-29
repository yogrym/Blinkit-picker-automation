package com.picker.BlinkitPicker.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Model.BookingTaskModel;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.BookingTaskRepo;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Services.Worker.BookingWorker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SchedulerServices {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServices.class);

    private static final long REFRESH_INTERVAL_MINUTES = 20;

    private BookingServices bookingServices;
    private BookingTaskRepo bookingTaskRepo;
    private UserRepo userRepo;
    private WebClientServices webClientServices;

    public SchedulerServices(BookingServices bookingServices, BookingTaskRepo bookingTaskRepo,
            UserRepo userRepo, WebClientServices webClientServices) {
        this.bookingServices = bookingServices;
        this.bookingTaskRepo = bookingTaskRepo;
        this.userRepo = userRepo;
        this.webClientServices = webClientServices;
    }

    /**
     * Runs every 10 minutes. For each active booking session, checks whether
     * 20 minutes have elapsed since the last token refresh. If so, refreshes
     * tokens directly inside the live BookingWorker thread (no stop/restart),
     * then persists the new tokens to the user record and updates lastRefreshedAt
     * on the task. All nulls and exceptions are handled — this method never crashes.
     */
    @Scheduled(fixedRate = 600000) // every 10 minutes
    public void proactivelyRefreshTokens() {
        logger.info("[Scheduler] Running token refresh check (threshold: {} min)...", REFRESH_INTERVAL_MINUTES);
        try {
            List<BookingTaskModel> activeTasks = bookingTaskRepo.findByActiveTrue();
            if (activeTasks == null || activeTasks.isEmpty()) {
                logger.info("[Scheduler] No active tasks found. Nothing to refresh.");
                return;
            }

            // Track which users have already had their tokens refreshed this cycle
            // to avoid duplicate API calls when a user has multiple active sessions
            Set<Long> refreshedUsers = new HashSet<>();

            for (BookingTaskModel task : activeTasks) {
                try {
                    if (task == null || task.getUserId() == null || task.getSessionId() == null) {
                        logger.warn("[Scheduler] Skipping task with null required fields.");
                        continue;
                    }

                    Long userId      = task.getUserId();
                    String sessionId = task.getSessionId();

                    // ── 1. Check 20-minute threshold ──────────────────────────────
                    LocalDateTime lastRefreshed = task.getLastRefreshedAt();
                    boolean needsRefresh = (lastRefreshed == null) ||
                            Duration.between(lastRefreshed, LocalDateTime.now()).toMinutes() >= REFRESH_INTERVAL_MINUTES;

                    if (!needsRefresh) {
                        logger.debug("[Scheduler] Session {} for user {} was refreshed recently — skipping.",
                                sessionId, userId);
                        continue;
                    }

                    // ── 2. Look up the live in-memory worker ──────────────────────
                    BookingWorker worker = bookingServices.getWorkerForSession(userId, sessionId);
                    if (worker == null) {
                        logger.warn("[Scheduler] No live worker found for session {} user {} — skipping.",
                                sessionId, userId);
                        continue;
                    }

                    // ── 3. Refresh tokens in-thread (once per user per scheduler cycle) ──
                    boolean refreshed;
                    if (!refreshedUsers.contains(userId)) {
                        // First session for this user this cycle — do the actual token refresh
                        refreshed = worker.refreshTokensFromScheduler(webClientServices);
                        if (refreshed) {
                            refreshedUsers.add(userId);
                        }
                    } else {
                        // Another session for the same user — the worker's headers object
                        // was already updated by the first call above, so just mark as done
                        refreshed = true;
                    }

                    // ── 4. Persist to DB on success ───────────────────────────────
                    if (refreshed) {
                        // Save the new tokens to the user record in DB
                        UserModel user = userRepo.findById(userId).orElse(null);
                        if (user == null) {
                            logger.warn("[Scheduler] User {} not found in DB — skipping token DB save.", userId);
                        } else {
                            UserHeaderModel headers = worker.getHeaders();
                            if (headers == null) {
                                logger.warn("[Scheduler] Worker headers null for session {} user {} — skipping DB save.",
                                        sessionId, userId);
                            } else {
                                user.setUserHeaders(headers);
                                userRepo.save(user);
                                logger.info("[Scheduler] New tokens saved to DB for user {}.", userId);
                            }
                        }

                        // Update lastRefreshedAt and the latest tokens on the task record in DB
                        task.setLastRefreshedAt(LocalDateTime.now());
                        task.setAccessToken(worker.getAccessToken());
                        task.setRefreshToken(worker.getRefreshToken());
                        bookingTaskRepo.save(task);
                        logger.info("[Scheduler] Token refresh complete for session {} user {}.", sessionId, userId);

                    } else {
                        logger.warn("[Scheduler] Token refresh failed for session {} user {} — will retry next cycle.",
                                sessionId, userId);
                    }

                } catch (Exception e) {
                    logger.error("[Scheduler] Error processing task {}: {}",
                            task != null ? task.getSessionId() : "null", e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("[Scheduler] Fatal error in proactivelyRefreshTokens: {}", e.getMessage(), e);
        }
    }
}
