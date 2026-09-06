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

    private final BookingServices   bookingServices;
    private final BookingTaskRepo   bookingTaskRepo;
    private final UserRepo          userRepo;
    private final WebClientServices webClientServices;

    public SchedulerServices(BookingServices bookingServices, BookingTaskRepo bookingTaskRepo,
            UserRepo userRepo, WebClientServices webClientServices) {
        this.bookingServices   = bookingServices;
        this.bookingTaskRepo   = bookingTaskRepo;
        this.userRepo          = userRepo;
        this.webClientServices = webClientServices;
    }

    /**
     * Runs every 10 minutes.
     *
     * <p>For users <b>with active tasks</b>: checks {@code lastRefreshedAt} — if 20 min
     * have elapsed, refreshes their token once and pushes the result to every session
     * row in the DB, every live in-memory worker, and the UserModel row.
     *
     * <p>In both cases, if the UserModel row no longer exists in the database, all
     * in-memory workers and task rows for that user are immediately stopped and deleted
     * to prevent zombie threads consuming server resources.
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
                    processUserWithTasks(userId, userTasks);
                } catch (Exception e) {
                    logger.error("[Scheduler] Error processing token refresh for user {}: {}", userId, e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.error("[Scheduler] Fatal error in proactivelyRefreshTokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles the full token-refresh cycle for one user who has active sessions.
     *
     * <ol>
     *   <li>Checks if any session's {@code lastRefreshedAt} is ≥ 20 minutes old.
     *       If not, skips — no work needed.</li>
     *   <li>Fetches the user directly from the DB (single source of truth for credentials).
     *       If the user no longer exists, kills all their in-memory workers and deletes
     *       every task row, then returns.</li>
     *   <li>Calls the Cognito refresh API once using the DB user's credentials.</li>
     *   <li>Saves the new tokens back to the UserModel row.</li>
     *   <li>Pushes the new tokens to all task rows and all live in-memory workers via
     *       {@link BookingServices#propagateTokensToAllUserSessions}.</li>
     * </ol>
     */
    private void processUserWithTasks(Long userId, List<BookingTaskModel> userTasks) {

        // ── 1. Check if any session is overdue for a refresh ──────────────────────
        boolean anySessionNeedsRefresh = userTasks.stream().anyMatch(task -> {
            LocalDateTime lastRefreshed = task.getLastRefreshedAt();
            return (lastRefreshed == null)
                    || Duration.between(lastRefreshed, LocalDateTime.now()).toMinutes() >= REFRESH_INTERVAL_MINUTES;
        });

        if (!anySessionNeedsRefresh) {
            logger.debug("[Scheduler] All sessions for user {} were refreshed recently — skipping.", userId);
            return;
        }

        // ── 2. Fetch user from DB — this is the single source of truth ────────────
        UserModel user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            // User was deleted from the DB but their threads are still running in RAM.
            // Kill everything immediately to prevent zombie workers.
            logger.warn("[Scheduler] User {} not found in DB — stopping all sessions and cleaning up tasks.", userId);
            bookingServices.stopAndCleanAllSessionsForUser(userId);
            return;
        }

        // ── 3. Load credentials from the DB user row ──────────────────────────────
        UserHeaderModel headers      = user.getUserHeaders();
        String          refreshToken = (headers != null) ? headers.getRefreshToken() : null;

        if (headers == null || refreshToken == null) {
            logger.warn("[Scheduler] Cannot refresh: headers or refreshToken is null in DB for user {}.", userId);
            return;
        }

        // ── 4. Call Cognito refresh API once for this user ────────────────────────
        logger.info("[Scheduler] Refreshing token for user {} ({} active session(s)).", userId, userTasks.size());

        CognitoRefreshTokenRespons response = callRefreshApi(headers, refreshToken, userId);
        if (response == null) return; // already logged inside callRefreshApi

        String newAccessToken  = response.getAccessToken();
        String newRefreshToken = response.getRefreshToken();

        // ── 5. Persist new tokens to the UserModel row ────────────────────────────
        headers.setAccessToken(newAccessToken);
        headers.setRefreshToken(newRefreshToken);
        user.setUserHeaders(headers);
        userRepo.save(user);
        logger.info("[Scheduler] UserModel tokens updated in DB for user {}.", userId);

        // ── 6. Push new tokens to all task rows + all live in-memory workers ──────
        // This single call handles every session of the user atomically,
        // preserving paused/active state and updating lastRefreshedAt everywhere.
        bookingServices.propagateTokensToAllUserSessions(userId, newAccessToken, newRefreshToken);

        logger.info("[Scheduler] Token refresh complete for user {} — {} session(s) updated.", userId, userTasks.size());
    }

    /**
     * Calls the Cognito token-refresh API and returns the response.
     * Returns {@code null} if the call failed or the response was invalid/null tokens.
     */
    private CognitoRefreshTokenRespons callRefreshApi(UserHeaderModel headers, String refreshToken, Long userId) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", refreshToken);

        CognitoRefreshTokenRespons response;
        try {
            response = webClientServices.refreshToken(formData, headers);
        } catch (Exception e) {
            logger.error("[Scheduler] Cognito refresh API call failed for user {}: {}", userId, e.getMessage());
            return null;
        }

        if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
            logger.warn("[Scheduler] Cognito token refresh returned failure for user {}.", userId);
            return null;
        }

        if (response.getAccessToken() == null || response.getRefreshToken() == null) {
            logger.warn("[Scheduler] Cognito refresh returned null tokens for user {}.", userId);
            return null;
        }

        return response;
    }
}
