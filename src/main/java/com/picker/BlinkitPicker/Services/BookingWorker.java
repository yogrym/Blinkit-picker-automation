package com.picker.BlinkitPicker.Services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;

import com.picker.BlinkitPicker.Dto.Logs;
import com.picker.BlinkitPicker.Dto.Internal.BookSlotsRequest;
import com.picker.BlinkitPicker.Dto.request.FetchSlotsRequest;
import com.picker.BlinkitPicker.Dto.respons.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Dto.respons.FetchSlotsResponse;
import com.picker.BlinkitPicker.Dto.respons.GlobalRespons;
import com.picker.BlinkitPicker.Enums.RoleEnum;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.DateToUtc;
import com.picker.BlinkitPicker.Util.GenerateCookie;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@Getter
@Setter
@Slf4j
public class BookingWorker implements Runnable {

    private static final int MAX_IN_MEMORY_LOGS_SIZE = 20;
    private static final Logger logger =  LoggerFactory.getLogger(BookingWorker.class);
    private static final Logger fileLogger = LoggerFactory.getLogger("BOOKING_FILE");

    private String userId;
    private List<String> dates;
    private List<String> times;
    private volatile boolean isPaused = false;
    private volatile boolean isStop = false;

    private boolean isAdmin = false;

    private final UserModel user;
    private final UserRepo userRepo;

    private WebClientServices webClientServices;

    private final List<Logs> inMemoryUserLogs = Collections.synchronizedList(new ArrayList<>());
    private long bookedSlotsInSession = 0L;
    private boolean bookedSlotsSaved = false;

    private String jwtToken;
    private String refreshToken;

    public BookingWorker(String userId, List<String> dates, List<String> times, UserModel user,
            WebClientServices webClientServices, UserRepo userRepo) {
        this.userId = userId;
        this.dates = dates;
        this.times = times;
        this.user = user;
        this.userRepo = userRepo;
        this.jwtToken = user.getUserHeaders().getAuthorization();
        this.refreshToken = user.getRefreshToken();
        this.webClientServices = webClientServices;
    }

    public Boolean pause() {
        this.isPaused = true;
        return true;
    }

    public Boolean resume() {
        this.isPaused = false;
        return true;
    }

    public Boolean stop() {
        this.isStop = true;
        return true;
    }

    public void fecthSlots() {

        for (int i = 0; i < dates.size(); i++) {

            String cfbm = GenerateCookie.generateCfBmCookie();
            String requestId = GenerateCookie.generateRequestId();
            // Both http_session_token and session-token share the SAME UUID value per
            // session
            String sessionToken = GenerateCookie.generateSessionToken();
            String httpSessionToken = sessionToken;

            this.jwtToken = user.getUserHeaders().getAuthorization();
            this.refreshToken = user.getRefreshToken();
            String storeId = user.getUserHeaders().getSiteId();

            this.isAdmin = user.getRole().equals(RoleEnum.ADMIN);

            // Bot logic:
            // end_date = that day at 18:30 UTC (= midnight IST, start of that IST day)
            // start_date = end_date - 1 day
            String endDateUtc = DateToUtc.getDateToUtc(dates.get(i));
            String startDateUtc = DateToUtc.getPrevDateToUtc(endDateUtc);

            double xLat = 0.0;
            double xLong = 0.0;
            if (user.getUserHeaders() != null) {
                String latStr = user.getUserHeaders().getXLat();
                String longStr = user.getUserHeaders().getXLong();
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
            }

            FetchSlotsRequest request = FetchSlotsRequest.builder()
                    .endDate(endDateUtc)
                    .startDate(startDateUtc)
                    .locationInfo(FetchSlotsRequest.Location.builder()
                            .xLat(xLat)
                            .xLong(xLong)
                            .build())
                    .build();

           logger.info("slot fetching started start date {} : ", endDateUtc);

            try {
                UserHeaderModel headers = user.getUserHeaders();
                String role = user.getRole().toString();
                String employeeId = headers.getEmployeeId();
                String userAgent = headers.getUserAgent();
                String xDeviceId = headers.getXDeviceId();
                String siteId = headers.getSiteId();

                logger.debug("[BookingWorker - " + userId + "] Headers: " + headers.toString());

                final String fetchCfBm = cfbm;
                final String fetchRequestId = requestId;
                FetchSlotsResponse response = blockWithTokenRefresh(
                        "fetch slots",
                        () -> webClientServices.getSlotsDetails(
                                fetchCfBm, fetchRequestId, jwtToken, request,
                                siteId, employeeId, userAgent, xDeviceId,
                                role, sessionToken, httpSessionToken));
                logDebugToFile("[BookingWorker - " + userId + "] Raw API Response: " + response);

                // Returns slots in user's preferred time-window order (same as bot)
                // Key = slot ID, Value = IST time key e.g. "06:00-08:00"
                Map<String, String> slotIdToTime = filterSlotId(response, times);
                List<String> slotIds = new ArrayList<>(slotIdToTime.keySet());

                if (!slotIds.isEmpty()) {
                    String timesLog = slotIdToTime.toString();

                    cfbm = GenerateCookie.generateCfBmCookie();
                    requestId = GenerateCookie.generateRequestId();

                    final String bookCfBm = cfbm;
                    final String bookRequestId = requestId;
                    GlobalRespons bookingResponse = blockWithTokenRefresh(
                            "book slots",
                            () -> webClientServices.bookSlots(
                                    bookCfBm, bookRequestId, jwtToken,
                                    BookSlotsRequest.builder().slotIds(slotIds).build(),
                                    storeId, user, sessionToken, httpSessionToken, timesLog));

                    if (bookingResponse.isSuccess()) {
                        addInMemoryLog("SUCCESS! Booked slots " + slotIds + " on date " + dates.get(i));
                        incrementBookedSlots(slotIds.size());
                        continue;
                    } else {
                        // Bulk booking failed — retry each slot individually (same as bot's one-by-one
                        // fallback)
                        for (int j = 0; j < slotIds.size(); j++) {
                            String currentSlotId = slotIds.get(j);
                            List<String> singleSlot = new ArrayList<>();
                            singleSlot.add(currentSlotId);
                            String singleTimeLog = "{" + currentSlotId + "=" + slotIdToTime.get(currentSlotId) + "}";

                            cfbm = GenerateCookie.generateCfBmCookie();
                            requestId = GenerateCookie.generateRequestId();

                            final String retryCfBm = cfbm;
                            final String retryRequestId = requestId;
                            GlobalRespons bookingRetryResponse = blockWithTokenRefresh(
                                    "book single slot",
                                    () -> webClientServices.bookSlots(
                                            retryCfBm, retryRequestId, jwtToken,
                                            BookSlotsRequest.builder().slotIds(singleSlot).build(),
                                            storeId, user, sessionToken, httpSessionToken, singleTimeLog));

                            if (bookingRetryResponse.isSuccess()) {
                                addInMemoryLog("SUCCESS on retry! Booked " + currentSlotId
                                        + " on date " + dates.get(i));
                                incrementBookedSlots(1);
                            } else {
                                logToFile("[BookingWorker - " + userId + "] FAILED to book slot " + currentSlotId
                                        + " on date " + dates.get(i));
                            }
                        }
                    }

                }

            } catch (Throwable e) {
                logToFile("[BookingWorker - " + userId + "] Slot fetch/booking failed: " + e.toString());
            }
        }
    }

    private <T> T blockWithTokenRefresh(String operationName, Supplier<Mono<T>> apiCall) {
        try {
            return apiCall.get().block();
        } catch (WebClientResponseException e) {
            if (!isUnauthorizedOrForbidden(e)) {
                throw e;
            }

            logToFile("[BookingWorker - " + userId + "] " + operationName + " returned HTTP "
                    + e.getStatusCode().value() + ". Refreshing JWT token.");

            if (!refreshJwtToken()) {
                throw e;
            }

            logToFile("[BookingWorker - " + userId + "] JWT token refreshed. Retrying " + operationName + ".");
            return apiCall.get().block();
        }
    }

    private boolean isUnauthorizedOrForbidden(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        return statusCode == 401 || statusCode == 403;
    }

    private boolean refreshJwtToken() {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                logToFile("[BookingWorker - " + userId + "] Cannot refresh JWT token: refresh token is missing.");
                return false;
            }

            CognitoRefreshTokenRespons response = webClientServices.refreshToken(refreshToken);
            if (response == null || response.getAuthenticationResult() == null
                    || response.getAuthenticationResult().getIdToken() == null
                    || response.getAuthenticationResult().getIdToken().isBlank()) {
                logToFile(
                        "[BookingWorker - " + userId + "] Cannot refresh JWT token: Cognito response has no IdToken.");
                return false;
            }

            String freshToken = response.getAuthenticationResult().getIdToken();
            this.jwtToken = freshToken;
            user.setJwt(freshToken);
            if (user.getUserHeaders() != null) {
                user.getUserHeaders().setAuthorization(freshToken);
            }

            return true;
        } catch (Throwable e) {
            logToFile("[BookingWorker - " + userId + "] JWT refresh failed: " + e.toString());
            return false;
        }
    }

    /**
     * Filters available slots from the API response, matching the exact logic of
     * the bot:
     *
     * 1. Find the store whose ID matches the user's siteId in the response.
     * 2. From that store's slots, keep only slots where:
     * - is_booked == false
     * - booking_eligibility.allowed == true
     * 3. If times is ["all"] or null/empty → return ALL available slots.
     * 4. Otherwise, match each slot's IST time key ("HH:MM-HH:MM") against the
     * user's
     * preferred time windows IN ORDER, collecting all matches.
     *
     * Returns LinkedHashMap so that insertion order (= preference order) is
     * preserved.
     * Key = slot ID (String), Value = IST time key (e.g. "06:00-08:00").
     */
    private Map<String, String> filterSlotId(FetchSlotsResponse response, List<String> times) {
        if (response == null || response.getData() == null || response.getData().getStores() == null) {
            return Collections.emptyMap();
        }

        String userStoreId = user.getUserHeaders().getSiteId();
        logDebugToFile("[BookingWorker] Filtering slots for storeId: " + userStoreId + " | time_filter: " + times);

        // ── Step 1: find the matching store ──────────────────────────────────────
        FetchSlotsResponse.Store matchedStore = null;
        for (FetchSlotsResponse.Store store : response.getData().getStores()) {
            if (userStoreId != null && userStoreId.equals(store.getId())) {
                matchedStore = store;
                break;
            }
        }

        if (matchedStore == null) {
            logDebugToFile("[BookingWorker] Store " + userStoreId + " not found in API response.");
            return Collections.emptyMap();
        }

        if (matchedStore.getSlots() == null || matchedStore.getSlots().isEmpty()) {
            logDebugToFile("[BookingWorker] Store " + userStoreId + " has no slots in response.");
            return Collections.emptyMap();
        }

        // ── Step 2: keep only available + eligible slots (same as bot's raw_slots
        // filter) ──
        List<FetchSlotsResponse.Slot> availableSlots = new ArrayList<>();
        for (FetchSlotsResponse.Slot slot : matchedStore.getSlots()) {
            boolean isBooked = slot.isBooked();
            boolean allowed = slot.getBookingEligibility() != null && slot.getBookingEligibility().isAllowed();
            String timeKey = DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime());
            String label = DateToUtc.decodeTime(slot.getStartTime(), slot.getEndTime());

            logDebugToFile("[BookingWorker] Slot ID=" + slot.getId()
                    + " | timeKey=" + timeKey + " (" + label + ")"
                    + " | isBooked=" + isBooked + " | allowed=" + allowed);

            if (!isBooked && allowed) {
                availableSlots.add(slot);
            }
        }

        if (availableSlots.isEmpty()) {
            logDebugToFile("[BookingWorker] No available+eligible slots found for store " + userStoreId);
            return Collections.emptyMap();
        }

        // ── Step 3: "all" mode → return all available slots ──────────────────────
        boolean acceptAll = (times == null || times.isEmpty()
                || (times.size() == 1 && "all".equalsIgnoreCase(times.get(0))));

        if (acceptAll) {
            Map<String, String> all = new LinkedHashMap<>();
            for (FetchSlotsResponse.Slot slot : availableSlots) {
                all.put(String.valueOf(slot.getId()),
                        DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime()));
            }
            logDebugToFile("[BookingWorker] Mode=ALL -> returning " + all.size() + " available slots.");
            return all;
        }

        // ── Step 4: filter by preferred time windows IN ORDER ─────────────────────
        // Bot: for each key in time_filter, collect all slots whose time key matches.
        // Uses LinkedHashMap to preserve insertion order (= user preference order).
        Map<String, String> matchedSlots = new LinkedHashMap<>();
        for (String preferredKey : times) {
            for (FetchSlotsResponse.Slot slot : availableSlots) {
                if (DateToUtc.isTimeMatch(preferredKey, slot.getStartTime(), slot.getEndTime())) {
                    String slotKey = DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime());
                    matchedSlots.put(String.valueOf(slot.getId()), slotKey);
                    logDebugToFile("[BookingWorker] MATCHED slot " + slot.getId()
                            + " for time window " + preferredKey);
                }
            }
        }

        if (matchedSlots.isEmpty()) {
            // Log what we saw vs what we wanted — mirrors bot's debug log
            List<String> seenKeys = new ArrayList<>();
            for (FetchSlotsResponse.Slot slot : availableSlots) {
                seenKeys.add(DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime()));
            }
            logDebugToFile("[BookingWorker] No matching slots | Seen keys: " + seenKeys
                    + " | Filter: " + times);
        }

        return matchedSlots;
    }

    @Override
    public void run() {
        try {
            addInMemoryLog("Booking intilized for dates: " + dates);

            while (!this.isStop) {

                if (this.isPaused) {
                    try {
                        Thread.sleep(50000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                logDebugToFile("[BookingWorker - " + userId + "] Preparing to fetch slots. Found dates: " + dates);

                if (dates == null || dates.isEmpty()) {
                    this.isStop = true;
                    continue;
                }

                try {
                    fecthSlots();

                    if (Boolean.TRUE.equals(isAdmin)) {
                        Thread.sleep(10000);
                    } else {
                        Thread.sleep(40000);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable t) {
                    logToFile("[BookingWorker - " + userId + "] Worker stopped because of an error: " + t.toString());
                    break;
                }
            }
        } finally {
            saveBookedSlotsIfAny();
        }
    }

    public List<Logs> getLogs() {
        synchronized (inMemoryUserLogs) {
            return new ArrayList<>(inMemoryUserLogs);
        }
    }

    private void addInMemoryLog(String message) {
        synchronized (inMemoryUserLogs) {
            if (inMemoryUserLogs.size() >= MAX_IN_MEMORY_LOGS_SIZE) {
                inMemoryUserLogs.remove(0);
            }
            inMemoryUserLogs.add(Logs.builder()
                    .logs(List.of(message))
                    .build());
        }
    }

    private void incrementBookedSlots(long count) {
        if (count <= 0) return;
        bookedSlotsInSession += count;
        // Persist immediately so data is never lost on JVM kill/crash
        try {
            Long id = user.getId() != null ? user.getId() : Long.valueOf(userId);
            UserModel latestUser = userRepo.findById(id).orElse(user);
            Long currentTotal = latestUser.getTotalBookedSlots() != null ? latestUser.getTotalBookedSlots() : 0L;
            latestUser.setTotalBookedSlots(currentTotal + count);
            userRepo.save(latestUser);
        } catch (Exception e) {
            logToFile("[BookingWorker - " + userId + "] Failed to save booked slot count: " + e.toString());
        }
    }

    private void saveBookedSlotsIfAny() {
      
        if (bookedSlotsInSession <= 0) {
            return;
        }

        user.setTotalBookedSlots(user.getTotalBookedSlots() + bookedSlotsInSession);
        userRepo.save(user);
        bookedSlotsSaved = true;
    }

    private void logToFile(String message) {
        fileLogger.info(message);
    }

    private void logDebugToFile(String message) {
        fileLogger.debug(message);
    }

    public boolean removeOneDateFromList(String date) {
        dates.remove(date);
        return true;
    }

    public boolean removeOneTimeFromList(String time) {
        times.remove(time);
        return true;
    }
}
