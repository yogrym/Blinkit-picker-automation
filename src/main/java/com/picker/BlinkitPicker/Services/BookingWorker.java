package com.picker.BlinkitPicker.Services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.picker.BlinkitPicker.Dto.FetchSlotsRequest;
import com.picker.BlinkitPicker.Dto.FetchSlotsResponse;
import com.picker.BlinkitPicker.Dto.GlobalRespons;
import com.picker.BlinkitPicker.Dto.Logs;
import com.picker.BlinkitPicker.Dto.Internal.BookSlotsRequest;
import com.picker.BlinkitPicker.Enums.RoleEnum;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Util.DateToUtc;
import com.picker.BlinkitPicker.Util.GenerateCookie;

import reactor.core.publisher.Mono;

public class BookingWorker implements Runnable {

    private String userId;
    private List<String> dates;
    private List<String> times;
    private Boolean isPaused = false;
    private Boolean isStop = false;

    private Boolean isAdmin = false;

    private final UserModel user;

    private WebClientServices webClientServices;

    private List<Logs> logs = new ArrayList<>();

    private String jwtToken;
    private String refreshToken;

    public BookingWorker(String userId, List<String> dates, List<String> times, UserModel user,
            WebClientServices webClientServices) {
        this.userId = userId;
        this.dates = dates;
        this.times = times;
        this.user = user;
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

            FetchSlotsRequest request = FetchSlotsRequest.builder()
                    .endDate(endDateUtc)
                    .startDate(startDateUtc)
                    .locationInfo(FetchSlotsRequest.Location.builder()
                            .xLat(user.getUserHeaders().getXLat() != null
                                    ? Double.parseDouble(user.getUserHeaders().getXLat())
                                    : 0.0)
                            .xLong(user.getUserHeaders().getXLong() != null
                                    ? Double.parseDouble(user.getUserHeaders().getXLong())
                                    : 0.0)
                            .build())
                    .build();

            logToFile("[BookingWorker - " + userId + "] Fetching slots for storeId: " + storeId
                    + " | date: " + dates.get(i)
                    + " | endDate(UTC): " + endDateUtc
                    + " | startDate(UTC): " + startDateUtc
                    + " | time_filter: " + times);

            try {
                UserHeaderModel headers = user.getUserHeaders();
                String role = user.getRole().toString();
                String employeeId = headers.getEmployeeId();
                String userAgent = headers.getUserAgent();
                String xDeviceId = headers.getXDeviceId();
                String siteId = headers.getSiteId();

                logToFile("[BookingWorker - " + userId + "] Headers: " + headers.toString());
                logs.add(Logs.builder()
                        .logs(List.of("[BookingWorker - " + userId + "] Headers: " + headers.toString()))
                        .build());

                Mono<FetchSlotsResponse> responseMono = webClientServices.getSlotsDetails(
                        cfbm, requestId, jwtToken, request,
                        siteId, employeeId, userAgent, xDeviceId,
                        role, sessionToken, httpSessionToken);

                FetchSlotsResponse response = responseMono.block();
                logToFile("[BookingWorker - " + userId + "] Raw API Response: " + response);
                logs.add(Logs.builder()
                        .logs(List.of("[BookingWorker - " + userId + "] Raw API Response: " + response))
                        .build());

                // Returns slots in user's preferred time-window order (same as bot)
                // Key = slot ID, Value = IST time key e.g. "06:00-08:00"
                Map<String, String> slotIdToTime = filterSlotId(response, times);
                List<String> slotIds = new ArrayList<>(slotIdToTime.keySet());

                if (!slotIds.isEmpty()) {
                    String timesLog = slotIdToTime.toString();

                    cfbm = GenerateCookie.generateCfBmCookie();
                    requestId = GenerateCookie.generateRequestId();

                    Mono<GlobalRespons> responseBooking = webClientServices.bookSlots(
                            cfbm, requestId, jwtToken,
                            BookSlotsRequest.builder().slotIds(slotIds).build(),
                            storeId, user, sessionToken, httpSessionToken, timesLog);

                    if (responseBooking.block().isSuccess()) {
                        logToFile("[BookingWorker - " + userId + "] SUCCESS! Booked slots " + slotIds
                                + " on date " + dates.get(i));
                        logs.add(Logs.builder()
                                .logs(List.of("Slot booked successfully for " + slotIds + " on date " + dates.get(i)))
                                .build());
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

                            Mono<GlobalRespons> responseBookingRetry = webClientServices.bookSlots(
                                    cfbm, requestId, jwtToken,
                                    BookSlotsRequest.builder().slotIds(singleSlot).build(),
                                    storeId, user, sessionToken, httpSessionToken, singleTimeLog);

                            if (responseBookingRetry.block().isSuccess()) {
                                logToFile("[BookingWorker - " + userId + "] SUCCESS on retry! Booked "
                                        + currentSlotId + " on date " + dates.get(i));
                                logs.add(Logs.builder()
                                        .logs(List.of("Slot booked successfully for " + currentSlotId
                                                + " on date " + dates.get(i)))
                                        .build());
                            } else {
                                logToFile("[BookingWorker - " + userId + "] FAILED to book slot "
                                        + currentSlotId + " on date " + dates.get(i));
                                logs.add(Logs.builder()
                                        .logs(List.of("Failed to book slot for " + currentSlotId
                                                + " on date " + dates.get(i)))
                                        .build());
                            }
                        }
                    }

                } else {
                    logToFile("[BookingWorker - " + userId + "] No slots matched preferences for store "
                            + storeId + " on date " + dates.get(i));
                    logs.add(Logs.builder()
                            .logs(List.of("No slots available for " + storeId + " on date " + dates.get(i)))
                            .build());
                }

            } catch (Throwable e) {
                logToFile("[BookingWorker - " + userId + "] Error in fetching/booking slots: " + e.toString());
                logs.add(Logs.builder()
                        .logs(List.of("Error in fetching slots for " + storeId + " on date " + dates.get(i)))
                        .build());
                e.printStackTrace();
            }
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
        logToFile("[BookingWorker] Filtering slots for storeId: " + userStoreId + " | time_filter: " + times);

        // ── Step 1: find the matching store ──────────────────────────────────────
        FetchSlotsResponse.Store matchedStore = null;
        for (FetchSlotsResponse.Store store : response.getData().getStores()) {
            if (userStoreId != null && userStoreId.equals(store.getId())) {
                matchedStore = store;
                break;
            }
        }

        if (matchedStore == null) {
            logToFile("[BookingWorker] Store " + userStoreId + " not found in API response.");
            return Collections.emptyMap();
        }

        if (matchedStore.getSlots() == null || matchedStore.getSlots().isEmpty()) {
            logToFile("[BookingWorker] Store " + userStoreId + " has no slots in response.");
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

            logToFile("[BookingWorker] Slot ID=" + slot.getId()
                    + " | timeKey=" + timeKey + " (" + label + ")"
                    + " | isBooked=" + isBooked + " | allowed=" + allowed);

            if (!isBooked && allowed) {
                availableSlots.add(slot);
            }
        }

        if (availableSlots.isEmpty()) {
            logToFile("[BookingWorker] No available+eligible slots found for store " + userStoreId);
            logs.add(Logs.builder()
                    .logs(List.of("[BookingWorker] No available+eligible slots found for store " + userStoreId))
                    .build());
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
            logToFile("[BookingWorker] Mode=ALL → returning " + all.size() + " available slots.");
            logs.add(Logs.builder()
                    .logs(List.of("[BookingWorker] Mode=ALL → returning " + all.size() + " available slots."))
                    .build());
            return all;
        }

        // ── Step 4: filter by preferred time windows IN ORDER ─────────────────────
        // Bot: for each key in time_filter, collect all slots whose time key matches.
        // Uses LinkedHashMap to preserve insertion order (= user preference order).
        Map<String, String> matchedSlots = new LinkedHashMap<>();
        for (String preferredKey : times) {
            for (FetchSlotsResponse.Slot slot : availableSlots) {
                String slotKey = DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime());
                if (preferredKey != null && preferredKey.trim().equalsIgnoreCase(slotKey)) {
                    matchedSlots.put(String.valueOf(slot.getId()), slotKey);
                    logToFile("[BookingWorker] MATCHED slot " + slot.getId()
                            + " for time window " + preferredKey);
                    logs.add(Logs.builder()
                            .logs(List.of("[BookingWorker] MATCHED slot " + slot.getId()
                                    + " for time window " + preferredKey))
                            .build());
                }
            }
        }

        if (matchedSlots.isEmpty()) {
            // Log what we saw vs what we wanted — mirrors bot's debug log
            List<String> seenKeys = new ArrayList<>();
            for (FetchSlotsResponse.Slot slot : availableSlots) {
                seenKeys.add(DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime()));
            }
            logToFile("[BookingWorker] No matching slots | Seen keys: " + seenKeys
                    + " | Filter: " + times);
            logs.add(Logs.builder()
                    .logs(List.of("[BookingWorker] No matching slots | Seen keys: " + seenKeys
                            + " | Filter: " + times))
                    .build());
        }

        return matchedSlots;
    }

    @Override
    public void run() {
        logToFile("[BookingWorker - " + userId + "] Background thread started successfully!");
        logs.add(Logs.builder()
                .logs(List.of("[BookingWorker - " + userId + "] Background thread started successfully!"))
                .build());

        while (!this.isStop) {

            if (this.isPaused) {
                try {
                    Thread.sleep(50000);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }

            logToFile("[BookingWorker - " + userId + "] Preparing to fetch slots. Found dates: " + dates);
            logs.add(Logs.builder()
                    .logs(List.of("[BookingWorker - " + userId + "] Preparing to fetch slots. Found dates: " + dates))
                    .build());

            if (dates == null || dates.isEmpty()) {
                logToFile("[BookingWorker - " + userId
                        + "] ERROR: The 'dates' array is null or empty! Cannot fetch slots. Stopping worker.");
                logs.add(Logs.builder()
                        .logs(List.of("[BookingWorker - " + userId
                                + "] ERROR: The 'dates' array is null or empty! Cannot fetch slots. Stopping worker."))
                        .build());
                this.isStop = true;
                continue;
            }

            try {
                fecthSlots();

                if (Boolean.TRUE.equals(isAdmin)) {
                    Thread.sleep(100);
                } else {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable t) {
                logToFile("[BookingWorker - FATAL] Thread crashed: " + t.toString());
                t.printStackTrace();
                break;
            }
        }
    }

    public List<Logs> getLogs() {
        return logs;
    }

    private void logToFile(String message) {
        System.out.println(message);
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("booking_worker.log"),
                    java.time.LocalDateTime.now() + " - " + message + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.out.println("Failed to write to log file: " + e.getMessage());
        }
    }
}
