package com.picker.BlinkitPicker.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Dto.DateAndTimeList.TimesList;
import com.picker.BlinkitPicker.Dto.DateAndTimeList.UserRequestedDateAndTime;
import com.picker.BlinkitPicker.Dto.request.BookingRequest;
import com.picker.BlinkitPicker.Dto.respons.AvailableSlotsRespons;
import com.picker.BlinkitPicker.Dto.respons.LogsResponse;
import com.picker.BlinkitPicker.Dto.respons.SessionDateTimeRespons;
import com.picker.BlinkitPicker.Dto.Logs;
import com.picker.BlinkitPicker.Model.BookingTaskModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Model.BookingTaskModel.SessionInformation;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Repository.BookingTaskRepo;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Services.Worker.BookingWorker;
import com.picker.BlinkitPicker.Dto.Internal.ViewAvailaibleSlotsRequest;
import com.picker.BlinkitPicker.Util.DateToUtc;
import com.picker.BlinkitPicker.Util.GenerateCookie;
import com.picker.BlinkitPicker.Util.SessionIdGenerator;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BookingServices implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(BookingServices.class);

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final ConcurrentHashMap<String, WorkerList> workerMap = new ConcurrentHashMap<>();
    private LinkedHashMap<String,LinkedHashSet<TimesList>> refrence = new LinkedHashMap<>();

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BookingTaskRepo bookingTaskRepo;

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private WebClientServices webClientServices;

    @Override
    public void run(ApplicationArguments args) {
        restoreActiveBookings();
    }

    public WorkerList.BookingData startBooking(String token, BookingRequest request) {
        System.out.println("[API POST /task/booking] Received booking request for dates: " + request.getDates()
                + ", times: " + request.getTime());

        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            System.out.println("[API POST /task/booking] FAILED: JWT token is expired or invalid.");
            throw new RuntimeException("JWT token is expired or invalid");
        }

        Long userId = claims.get("userId", Long.class);
        System.out.println("[API POST /task/booking] Authenticated userId: " + userId);

        UserModel user = userRepo.findById(userId).orElseThrow(() -> {
            System.out.println("[API POST /task/booking] FAILED: User not found in DB.");
            return new RuntimeException("User not found");
        });

        List<String> dates = request.getDates();
        List<String> times = request.getTime();

        String sessionId = SessionIdGenerator.generateSessionId();
        // generate a converted date and times list for the user 
        
        convertAllToUtc(request,refrence);
        UserRequestedDateAndTime dateAndTime = UserRequestedDateAndTime.builder()
                                               .DateAndTime(refrence)
                                               .build();
        

        BookingWorker worker = new BookingWorker(
                user.getUserHeaders(),
                user.getUserHeaders().getAccessToken(),
                user.getUserHeaders().getRefreshToken(),
                webClientServices,
                dateAndTime,
                user.getRole() != null && (user.getRole() == com.picker.BlinkitPicker.Enums.RoleEnum.ADMIN
                        || user.getRole() == com.picker.BlinkitPicker.Enums.RoleEnum.MAINTAINER));


        String firstDate = dates != null && !dates.isEmpty() ? dates.get(0) : null;
        String lastDate = dates != null && !dates.isEmpty() ? dates.get(dates.size() - 1) : null;

        UserHeaderModel userHeaders = user.getUserHeaders();
        String initialAccessToken  = userHeaders != null ? userHeaders.getAccessToken()  : null;
        String initialRefreshToken = userHeaders != null ? userHeaders.getRefreshToken() : null;

        bookingTaskRepo.save(BookingTaskModel.builder()
                .sessionId(sessionId)
                .userId(userId)
                .sessionInfo(SessionInformation.builder().sessionId(sessionId).dates(dates).times(times).build())
                .paused(false)
                .active(true)
                .firstDate(firstDate)
                .lastDate(lastDate)
                .accessToken(initialAccessToken)
                .refreshToken(initialRefreshToken)
                .build());

        addWorkerToMemory(userId.toString(), sessionId, worker, false, firstDate, lastDate);

        return new WorkerList.BookingData(false, sessionId, firstDate, lastDate);
    }


     private void  convertAllToUtc(BookingRequest request,
        LinkedHashMap<String,LinkedHashSet<TimesList>> emptyHashMap) {

        if(request==null){
            return ;
        }

        List<String> dates = request.getDates();
        List<String> times = request.getTime();
        

        for(int i =0;i<dates.size();i++) {
          LinkedHashSet<TimesList> t = new LinkedHashSet<>();
          TimesList tBuildList = TimesList.builder()
                                 .times(times)
                                 .build();
          t.add(tBuildList);

         refrence.put(DateToUtc.getDateToUtc(dates.get(i)), t);

        }
         
    }

    public WorkerList.BookingData startBookingFromSheduler(BookingTaskModel task, UserModel user) {
        String sessionId = task.getSessionId();
        List<String> dates = task.getSessionInfo().getDates();
        List<String> times = task.getSessionInfo().getTimes();

        // Rebuild UserRequestedDateAndTime from the persisted flat dates + times
        LinkedHashMap<String, LinkedHashSet<TimesList>> map = new LinkedHashMap<>();
        if (dates != null) {
            LinkedHashSet<TimesList> timesSet = new LinkedHashSet<>();
            timesSet.add(TimesList.builder().times(times).build());
            for (String date : dates) {
                map.put(DateToUtc.getDateToUtc(date), new LinkedHashSet<>(timesSet));
            }
        }
        UserRequestedDateAndTime dateAndTime = UserRequestedDateAndTime.builder().DateAndTime(map).build();

        UserHeaderModel headers = user.getUserHeaders();
        BookingWorker worker = new BookingWorker(
                headers,
                headers != null ? headers.getAccessToken()  : null,
                headers != null ? headers.getRefreshToken() : null,
                webClientServices,
                dateAndTime,
                user.getRole() != null && (user.getRole() == com.picker.BlinkitPicker.Enums.RoleEnum.ADMIN
                        || user.getRole() == com.picker.BlinkitPicker.Enums.RoleEnum.MAINTAINER));

        Boolean isPaused = task.getPaused() != null ? task.getPaused() : false;
        if (isPaused) {
            worker.pause();
        }

        addWorkerToMemory(user.getId().toString(), sessionId, worker, isPaused, task.getFirstDate(), task.getLastDate());

        return new WorkerList.BookingData(isPaused, sessionId, task.getFirstDate(), task.getLastDate());
    }

    public List<WorkerList.BookingData> getBookingData(String token) {
        Long userId = jwtServices.extractUserId(token);
        if (workerMap.containsKey(userId.toString())) {
            return workerMap.get(userId.toString()).getAllBookingData();
        }
        return Collections.emptyList();
    }

    public boolean forceWorkerAccessTokenRefresh(String token, String sessionId) {
        // Token refresh is now handled entirely by the centralized scheduler.
        // Returning true to satisfy legacy frontend UI buttons without breaking them.
        return true;
    }


    public String stopBooking(String token, String sessionId) {
        Long userId = jwtServices.extractUserId(token);
        
        // Always try to delete from DB first, in case the worker isn't in memory
        BookingTaskModel bookingTask = bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId).orElse(null);
        if (bookingTask != null) {
            bookingTaskRepo.delete(bookingTask);
        }

        if (workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                worker.stop();
                userWorkers.removeWorker(sessionId);
                return "Stopped " + sessionId;
            }
        } 

        if (bookingTask != null) {
            return "Stopped " + sessionId;
        }

        return "OOPS! Session not found. It may have already been stopped or never existed.";
        
    }

     public String stopBookingFromSheduler(String sessionId, Long userId) {
        // 1. Stop thread and remove from memory
        if (workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                worker.stop();
                userWorkers.removeWorker(sessionId);
            }
        }
        // 2. Delete from DB so ApplicationRunner won't restore this session on next restart
        bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId)
                .ifPresent(bookingTaskRepo::delete);

        logger.info("[BookingServices] Session {} for user {} fully cleaned up (memory + DB).", sessionId, userId);
        return "Stopped " + sessionId;
    }

    /**
     * Stops and removes every in-memory worker for a user, then deletes ALL
     * their task rows (active or paused) from the database.
     *
     * <p>Called by the scheduler when the user's DB record no longer exists —
     * prevents "zombie" threads continuing to run for a deleted user.
     *
     * @param userId the internal DB user ID whose sessions should be killed
     */
    public void stopAndCleanAllSessionsForUser(Long userId) {
        if (userId == null) return;

        // 1. Stop every in-memory worker for this user
        WorkerList userWorkers = workerMap.get(userId.toString());
        if (userWorkers != null) {
            userWorkers.getAllWorkers().forEach((sessionId, worker) -> {
                try {
                    worker.stop();
                    logger.info("[BookingServices] Stopped zombie worker for user {} session {}.", userId, sessionId);
                } catch (Exception e) {
                    logger.error("[BookingServices] Error stopping worker for session {}: {}", sessionId, e.getMessage());
                }
            });
            workerMap.remove(userId.toString());
        }

        // 2. Delete ALL task rows (active or paused) from the DB for this user
        List<BookingTaskModel> allTasks = bookingTaskRepo.findByUserId(userId);
        if (allTasks != null && !allTasks.isEmpty()) {
            bookingTaskRepo.deleteAll(allTasks);
            logger.info("[BookingServices] Deleted {} task row(s) for non-existent user {}.", allTasks.size(), userId);
        }
    }

    public String pauseBooking(String token, String sessionId) {
        Long userId = jwtServices.extractUserId(token);
        if (workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                if (worker.pause()) {
                    WorkerList.BookingData bookingData = userWorkers.getBookingDataMap().get(sessionId);
                    bookingData.setIsPaused(true);
                    bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId).ifPresent(task -> {
                        task.setPaused(true);
                        bookingTaskRepo.save(task);
                    });
                }
                return "Paused " + sessionId;
            }
        }
        if (setTaskPaused(userId, sessionId, true)) {
            return "Paused " + sessionId;
        }
        return "Session not found";
    }

    public String resumeBooking(String token, String sessionId) {
        Long userId = jwtServices.extractUserId(token);
        if (workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                if (worker.resume()) {
                    WorkerList.BookingData bookingData = userWorkers.getBookingDataMap().get(sessionId);
                    bookingData.setIsPaused(false);
                    bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId).ifPresent(task -> {
                        task.setPaused(false);
                        bookingTaskRepo.save(task);
                    });
                }
                return "Resumed " + sessionId;
            }
        }
        if (setTaskPaused(userId, sessionId, false)) {
            return "Resumed " + sessionId;
        }
        return "Session not found";
    }

    public String changeStoreId(String token, String newStoreId) {
        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            throw new RuntimeException("JWT token is expired or invalid");
        }
        Long userId = claims.get("userId", Long.class);
        UserModel user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getUserHeaders() != null) {
            user.getUserHeaders().setSiteId(newStoreId);
            userRepo.save(user);
            return "Store ID successfully updated to " + newStoreId;
        } else {
            throw new RuntimeException("User headers not configured for this user");
        }
    }

    public ConcurrentHashMap<String, WorkerList> getWorkerMap() {
        return workerMap;
    }

    /**
     * Returns the live BookingWorker for the given user/session, or null if not found.
     * Used by the scheduler to refresh tokens in-thread without restarting the booking.
     */
    public BookingWorker getWorkerForSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null) return null;
        WorkerList userWorkers = workerMap.get(userId.toString());
        if (userWorkers == null) return null;
        return userWorkers.getWorker(sessionId);
    }

    /**
     * Propagates freshly obtained tokens to EVERY active session of a user — both the
     * in-memory BookingWorkers and the persisted BookingTaskModel rows.
     *
     * <p>Rules:
     * <ul>
     *   <li>Paused sessions stay paused — only tokens are updated.</li>
     *   <li>Active sessions stay active — only tokens are updated.</li>
     *   <li>lastRefreshedAt is stamped on all DB tasks so the scheduler does not
     *       immediately re-refresh these sessions in the same cycle.</li>
     * </ul>
     *
     * <p>Called from two places:
     * <ol>
     *   <li>{@code AuthServices.verifyOtp()} — when the user logs in with a fresh OTP.</li>
     *   <li>{@code SchedulerServices.proactivelyRefreshTokens()} — after the Cognito
     *       token-refresh call succeeds for this user.</li>
     * </ol>
     *
     * @param userId        the internal DB user ID
     * @param accessToken   the new Blinkit access token
     * @param refreshToken  the new Blinkit refresh token
     */
    public void propagateTokensToAllUserSessions(Long userId, String accessToken, String refreshToken) {
        if (userId == null || accessToken == null || refreshToken == null) {
            logger.warn("[BookingServices] propagateTokensToAllUserSessions called with null args for userId={}", userId);
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        // ── 1. Update every live in-memory worker for this user ──────────────────
        WorkerList userWorkers = workerMap.get(userId.toString());
        if (userWorkers != null) {
            userWorkers.getAllWorkers().forEach((sessionId, worker) -> {
                try {
                    worker.setAccessToken(accessToken);
                    worker.setRefreshToken(refreshToken);
                    if (worker.getHeaders() != null) {
                        worker.getHeaders().setAccessToken(accessToken);
                        worker.getHeaders().setRefreshToken(refreshToken);
                    }
                    worker.setLastRefreshedAt(now);
                    logger.info("[BookingServices] Token updated in-memory for user {} session {}", userId, sessionId);
                } catch (Exception e) {
                    logger.error("[BookingServices] Failed to update in-memory worker for session {}: {}", sessionId, e.getMessage());
                }
            });
        } else {
            logger.debug("[BookingServices] No in-memory workers found for user {} — DB-only update.", userId);
        }

        // ── 2. Update every active DB task row for this user ─────────────────────
        List<BookingTaskModel> userTasks = bookingTaskRepo.findByUserIdAndActiveTrue(userId);
        if (userTasks != null && !userTasks.isEmpty()) {
            for (BookingTaskModel task : userTasks) {
                try {
                    task.setAccessToken(accessToken);
                    task.setRefreshToken(refreshToken);
                    task.setLastRefreshedAt(now);
                    bookingTaskRepo.save(task);
                    logger.info("[BookingServices] Token persisted to DB for user {} session {}", userId, task.getSessionId());
                } catch (Exception e) {
                    logger.error("[BookingServices] Failed to persist tokens for session {}: {}", task.getSessionId(), e.getMessage());
                }
            }
        } else {
            logger.debug("[BookingServices] No active DB tasks found for user {} — skipping DB token update.", userId);
        }
    }

    
    public void applyShuffleToAllUsers(long minMs, long maxMs) {
        java.util.Random rng = new java.util.Random();
        workerMap.forEach((userId, workerList) ->
            workerList.getAllWorkers().forEach((sessionId, worker) -> {
                long sleepMs = minMs + (long)(rng.nextDouble() * (maxMs - minMs));
                worker.enableShuffle(sleepMs);
                logger.debug("[BookingServices] Shuffle ON for user={} session={} sleepMs={}", userId, sessionId, sleepMs);
            })
        );
        logger.info("[BookingServices] Shuffle mode ENABLED for all workers (range: {}–{} ms).", minMs, maxMs);
    }

   
    public void disableShuffleForAllUsers() {
        workerMap.forEach((userId, workerList) ->
            workerList.getAllWorkers().forEach((sessionId, worker) -> {
                worker.disableShuffle();
                logger.debug("[BookingServices] Shuffle OFF for user={} session={}", userId, sessionId);
            })
        );
        logger.info("[BookingServices] Shuffle mode DISABLED for all workers.");
    }

    public LogsResponse getSessionLogs(String token, String sessionId, int afterIndex) {
        Long userId = jwtServices.extractUserId(token);
        if (userId != null && workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            if (userWorkers != null) {
                BookingWorker worker = userWorkers.getWorker(sessionId);
                if (worker != null) {
                    List<Logs> allLogs = worker.getLogs();
                    boolean isReset = true;
                    List<Logs> logsToReturn;

                    if (afterIndex >= 0 && afterIndex < allLogs.size()) {
                        logsToReturn = allLogs.subList(afterIndex + 1, allLogs.size());
                        isReset = false;
                    } else {
                        logsToReturn = allLogs;
                        isReset = true;
                    }

                    return LogsResponse.builder()
                            .logs(logsToReturn)
                            .isReset(isReset)
                            .build();
                }
            }
        }
        return LogsResponse.builder()
                .logs(Collections.emptyList())
                .isReset(true)
                .build();
    }

    private void restoreActiveBookings() {
        List<BookingTaskModel> activeTasks = bookingTaskRepo.findByActiveTrue();
        for (BookingTaskModel task : activeTasks) {
            try {
                UserModel user = userRepo.findById(task.getUserId()).orElse(null);
                if (user == null) {
                    System.out.println("[BookingServices] Skipping restored session " + task.getSessionId()
                            + ": user not found.");
                    continue;
                }

                // Use the task's tokens if present — they are the most up-to-date
                // (kept in sync by the scheduler), more reliable than user.userHeaders on restart
                if (task.getAccessToken() != null && task.getRefreshToken() != null
                        && user.getUserHeaders() != null) {
                    user.getUserHeaders().setAccessToken(task.getAccessToken());
                    user.getUserHeaders().setRefreshToken(task.getRefreshToken());
                }

                // Rebuild UserRequestedDateAndTime from persisted flat dates + times
                List<String> rDates = task.getSessionInfo().getDates();
                List<String> rTimes = task.getSessionInfo().getTimes();
                LinkedHashMap<String, LinkedHashSet<TimesList>> rMap = new LinkedHashMap<>();
                if (rDates != null) {
                    LinkedHashSet<TimesList> rTimesSet = new LinkedHashSet<>();
                    rTimesSet.add(TimesList.builder().times(rTimes).build());
                    for (String d : rDates) {
                        rMap.put(DateToUtc.getDateToUtc(d), new LinkedHashSet<>(rTimesSet));
                    }
                }
                UserRequestedDateAndTime rDateAndTime = UserRequestedDateAndTime.builder().DateAndTime(rMap).build();

                UserHeaderModel rHeaders = user.getUserHeaders();
                BookingWorker worker = new BookingWorker(
                        rHeaders,
                        rHeaders != null ? rHeaders.getAccessToken()  : null,
                        rHeaders != null ? rHeaders.getRefreshToken() : null,
                        webClientServices,
                        rDateAndTime,
                        user.getRole() != null && (user.getRole() == com.picker.BlinkitPicker.Enums.RoleEnum.ADMIN
                                || user.getRole() == com.picker.BlinkitPicker.Enums.RoleEnum.MAINTAINER));

                Boolean isPaused = task.getPaused() != null ? task.getPaused() : false;
                if (isPaused) {
                    worker.pause();
                }

                addWorkerToMemory(
                        task.getUserId().toString(),
                        task.getSessionId(),
                        worker,
                        Boolean.TRUE.equals(task.getPaused()),
                        task.getFirstDate(),
                        task.getLastDate());

                System.out.println("[BookingServices] Restored booking session " + task.getSessionId()
                        + " for user " + task.getUserId());
            } catch (Exception e) {
                System.out.println("[BookingServices] Failed to restore booking session " + task.getSessionId()
                        + ": " + e.getMessage());
            }
        }
    }

    private void addWorkerToMemory(String userId, String sessionId, BookingWorker worker, Boolean isPaused,
                                   String firstDate, String lastDate) {
        WorkerList userWorkers = workerMap.computeIfAbsent(userId, key -> new WorkerList());
        userWorkers.addWorker(sessionId, worker);
        userWorkers.addBookingData(isPaused, sessionId, firstDate, lastDate);
        executor.submit(worker);
    }


    /* private boolean markTaskInactive(Long userId, String sessionId) {
        return bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId)
                .map(task -> {
                    task.setActive(false);
                    bookingTaskRepo.save(task);
                    return true;
                })
                .orElse(false);
    } */
   

    private boolean setTaskPaused(Long userId, String sessionId, boolean paused) {
        return bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId)
                .map(task -> {
                    task.setPaused(paused);
                    bookingTaskRepo.save(task);
                    return true;
                })
                .orElse(false);
    }

    public boolean removeDate(String date, String time, String token, String sessionId) {

        if (token != null && token.startsWith("Bearer")) {
            token = token.substring(7).trim();
        }

        Long userId = jwtServices.extractUserId(token);

        if (userId == null && date == null) {
            return false;
        }

        WorkerList userWorker = workerMap.get(userId.toString());
        if (userWorker != null) {
            BookingWorker bookingWorker = userWorker.getWorker(sessionId);
            if (bookingWorker != null) {
                if (date != null && time != null) {
                    bookingWorker.removeOneTimeFromDate(date, time);
                } else if (date != null) {
                    bookingWorker.removeOneDateFromList(date);
                }
            }
        }
        
        bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId).ifPresent(task -> {
            boolean updated = false;
            if (date != null && task.getSessionInfo().getDates() != null) {
                task.getSessionInfo().getDates().remove(date);
                updated = true;
            }

            if (time != null && task.getSessionInfo().getTimes() != null) {
                task.getSessionInfo().getTimes().remove(time);
                updated = true;
            }
            if (updated) {
                bookingTaskRepo.save(task);
            }
        });

        return true;
    }

    public SessionDateTimeRespons getSessionTimeAndDate(String token, String sessionId) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = jwtServices.extractUserId(token);

        if (userId == null) {
            throw new RuntimeException("Invalid token");
        }

        WorkerList userWorker = workerMap.get(userId.toString());
        if (userWorker != null) {
            BookingWorker bookingWorker = userWorker.getWorker(sessionId);
            if (bookingWorker != null) {
                return com.picker.BlinkitPicker.Dto.respons.SessionDateTimeRespons.builder()
                        .dates(bookingWorker.getDates())
                        .times(bookingWorker.getTimes())
                        .build();
            }
        }

        var task = bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        return SessionDateTimeRespons.builder()
                .dates(task.getSessionInfo().getDates())
                .times(task.getSessionInfo().getTimes())
                .build();
    }

    public AvailableSlotsRespons getAvailableSlots(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Long userId = jwtServices.extractUserId(token);

        if (userId == null) {
            throw new RuntimeException("Invalid token");
        } else {
            UserModel user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

            UserHeaderModel headers = user.getUserHeaders();
            if (headers == null) {
                throw new RuntimeException("User headers are not configured. Please login again.");
            }

            double xLat = 0.0;
            double xLong = 0.0;
            String latStr = headers.getXLat();
            String longStr = headers.getXLong();
            if (latStr != null && !latStr.isBlank()) {
                try {
                    xLat = Double.parseDouble(latStr.trim());
                } catch (NumberFormatException ignored) {}
            }
            if (longStr != null && !longStr.isBlank()) {
                try {
                    xLong = Double.parseDouble(longStr.trim());
                } catch (NumberFormatException ignored) {}
            }

            ViewAvailaibleSlotsRequest request = ViewAvailaibleSlotsRequest.builder()
                .locationInfo(ViewAvailaibleSlotsRequest.LocationInfo.builder()
                    .xLat(xLat)
                    .xLong(xLong)
                    .placeId("")
                    .placeName("")
                    .build())
                .build();

            Mono<AvailableSlotsRespons> response = webClientServices.getAvailableSlots(
                GenerateCookie.generateCfBmCookie(),
                GenerateCookie.generateRequestId(),
                GenerateCookie.generateHttpSessionToken(),
                GenerateCookie.generateSessionToken(),
                headers.getSiteId(),
                user,
                request,
                headers.getAccessToken(),
                headers.getUserAgent()
            );

            return response.block();
        }
    }

}
