package com.picker.BlinkitPicker.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.respons.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Model.BookingTaskModel;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.BookingTaskRepo;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Services.Worker.BookingWorker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class SchedulerServices {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerServices.class);

    private static final long REFRESH_INTERVAL_MINUTES = 20;

    private final BookingServices bookingServices;
    private final BookingTaskRepo bookingTaskRepo;
    private final UserRepo userRepo;
    private final WebClientServices webClientServices;

    public SchedulerServices(BookingServices bookingServices, BookingTaskRepo bookingTaskRepo,
            UserRepo userRepo, WebClientServices webClientServices) {
        this.bookingServices = bookingServices;
        this.bookingTaskRepo = bookingTaskRepo;
        this.userRepo = userRepo;
        this.webClientServices = webClientServices;
    }

    /**
     * Runs every 10 minutes.
     *
     * <p><b>Correct multi-session logic:</b>
     * <ol>
     *   <li>Load all active {@link BookingTaskModel} rows.</li>
     *   <li>Group them by {@code userId}.</li>
     *   <li>For each user, check the <em>oldest</em> {@code lastRefreshedAt} across all
     *       of that user's sessions. If any session is due for a refresh, refresh the
     *       token <strong>once</strong> (using any live worker of that user).</li>
     *   <li>On success, call {@link BookingServices#propagateTokensToAllUserSessions}
     *       which atomically updates <strong>all</strong> in-memory workers
     *       <strong>and</strong> all DB task rows for that user in one go.</li>
     *   <li>Also persist the new tokens to the {@link UserModel} row (the "user exists"
     *       guard from the old code is preserved — but token propagation happens
     *       regardless, via step 4).</li>
     * </ol>
     *
     * <p>This eliminates the stale-token bug where a session started after the
     * scheduler ran would have different tokens to the ones saved in the other sessions.
     * All sessions for a given user always share the same token pair.
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

            // ── Group all active tasks by userId ───────────────────────────────────
            Map<Long, List<BookingTaskModel>> tasksByUser = activeTasks.stream()
                    .filter(t -> t != null && t.getUserId() != null)
                    .collect(Collectors.groupingBy(
                            BookingTaskModel::getUserId,
                            LinkedHashMap::new,
                            Collectors.toList()));

            logger.info("[Scheduler] Found {} active task(s) across {} unique user(s).",
                    activeTasks.size(), tasksByUser.size());

            for (Map.Entry<Long, List<BookingTaskModel>> entry : tasksByUser.entrySet()) {
                Long userId = entry.getKey();
                List<BookingTaskModel> userTasks = entry.getValue();

                try {
                    processUserTokenRefresh(userId, userTasks);
                } catch (Exception e) {
                    logger.error("[Scheduler] Error processing token refresh for user {}: {}", userId, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("[Scheduler] Fatal error in proactivelyRefreshTokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles the token-refresh cycle for a single user's batch of active sessions.
     *
     * <p>Checks whether the <em>earliest</em> (most stale) {@code lastRefreshedAt}
     * across all sessions of this user has crossed the 20-minute threshold.
     * If so, performs a single Cognito token refresh and pushes the result to
     * every session at once via {@link BookingServices#propagateTokensToAllUserSessions}.
     */
    private void processUserTokenRefresh(Long userId, List<BookingTaskModel> userTasks) {

        // ── 1. Find the most stale lastRefreshedAt among all sessions ─────────────
        // If ANY session is overdue, we refresh for the whole user.
        boolean anySessionNeedsRefresh = userTasks.stream().anyMatch(task -> {
            LocalDateTime lastRefreshed = task.getLastRefreshedAt();
            return (lastRefreshed == null)
                    || Duration.between(lastRefreshed, LocalDateTime.now()).toMinutes() >= REFRESH_INTERVAL_MINUTES;
        });

        if (!anySessionNeedsRefresh) {
            logger.debug("[Scheduler] All sessions for user {} were refreshed recently — skipping.", userId);
            return;
        }

        // ── 2. Find any live in-memory worker for this user to perform the refresh ─
        // We only need one — the tokens are shared across all sessions.
        BookingWorker representativeWorker = null;
        for (BookingTaskModel task : userTasks) {
            BookingWorker w = bookingServices.getWorkerForSession(userId, task.getSessionId());
            if (w != null) {
                representativeWorker = w;
                break;
            }
        }

        if (representativeWorker == null) {
            logger.warn("[Scheduler] No live worker found for user {} — cannot refresh tokens.", userId);
            return;
        }

        // ── 3. Perform the actual Cognito token refresh (once per user) ────────────
        UserHeaderModel workerHeaders = representativeWorker.getHeaders();
        String currentRefreshToken = representativeWorker.getRefreshToken();

        if (workerHeaders == null || currentRefreshToken == null) {
            logger.warn("[Scheduler] Cannot refresh: headers or refreshToken null for user {}", userId);
            return;
        }

        logger.info("[Scheduler] Refreshing Cognito token for user {} ({} active session(s)).",
                userId, userTasks.size());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", currentRefreshToken);

        CognitoRefreshTokenRespons response;
        try {
            response = webClientServices.refreshToken(formData, workerHeaders);
        } catch (Exception e) {
            logger.error("[Scheduler] Cognito refresh API call failed for user {}: {}", userId, e.getMessage());
            return;
        }

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            logger.warn("[Scheduler] Cognito token refresh returned failure for user {}.", userId);
            return;
        }

        String newAccessToken  = response.getAccessToken();
        String newRefreshToken = response.getRefreshToken();

        if (newAccessToken == null || newRefreshToken == null) {
            logger.warn("[Scheduler] Cognito refresh returned null tokens for user {}.", userId);
            return;
        }

        // ── 4. Propagate new tokens → all in-memory workers + all DB task rows ────
        // This single call handles every session of the user atomically,
        // preserving paused/active state and updating lastRefreshedAt everywhere.
        bookingServices.propagateTokensToAllUserSessions(userId, newAccessToken, newRefreshToken);

        // ── 5. Persist new tokens to the UserModel row ────────────────────────────
        // The old "user exists" guard is intentionally preserved: if the user row
        // was somehow deleted, skip the user-model save but keep the task/worker
        // tokens updated (they were already updated in step 4).
        UserModel user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("[Scheduler] User {} not found in DB — skipping UserModel token save. " +
                    "Sessions are still updated.", userId);
        } else {
            UserHeaderModel updatedHeaders = representativeWorker.getHeaders();
            if (updatedHeaders != null) {
                // Mirror the new tokens into the header object stored on the user row
                updatedHeaders.setAccessToken(newAccessToken);
                updatedHeaders.setRefreshToken(newRefreshToken);
                user.setUserHeaders(updatedHeaders);
                userRepo.save(user);
                logger.info("[Scheduler] UserModel tokens updated for user {}.", userId);
            }
        }

        logger.info("[Scheduler] Token refresh complete for user {} — {} session(s) updated.",
                userId, userTasks.size());
    }
}
