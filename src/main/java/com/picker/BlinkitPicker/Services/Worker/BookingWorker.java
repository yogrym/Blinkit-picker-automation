package com.picker.BlinkitPicker.Services.Worker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.picker.BlinkitPicker.Dto.DateAndTimeList.TimesList;
import com.picker.BlinkitPicker.Dto.DateAndTimeList.UserRequestedDateAndTime;
import com.picker.BlinkitPicker.Dto.Internal.BookSlotsRequest;
import com.picker.BlinkitPicker.Dto.Logs;
import com.picker.BlinkitPicker.Dto.request.FetchSlotsRequest;
import com.picker.BlinkitPicker.Dto.respons.FetchSlotsResponse;
import com.picker.BlinkitPicker.Dto.respons.GlobalRespons;
import com.picker.BlinkitPicker.Model.UserHeaderModel;
import com.picker.BlinkitPicker.Services.WebClientServices;
import com.picker.BlinkitPicker.Util.DateToUtc;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;

/**
 * BookingWorker — one thread per user session.
 *
 * <p>Token refresh is handled entirely by the scheduler, which calls
 * {@code propagateTokensToAllUserSessions()} and updates {@link #headers},
 * {@link #accessToken}, and {@link #refreshToken} directly via Lombok setters.
 * This worker never refreshes tokens itself.
 *
 * <p>Each iteration fires fetch-slot requests for ALL pending dates in parallel,
 * books matched slots, and removes each booked time from that date's list.
 * When a date's time-list becomes empty the date is removed. When all dates
 * are gone the worker stops itself.
 *
 * <p>Two log channels:
 * <ul>
 *   <li><b>Terminal (SLF4J)</b> — printed ONCE at start and once per booking success.</li>
 *   <li><b>User logs</b>        — max 20 entries; written only on booking success.
 *                                 Pinned "working fine" entry is always shown first.</li>
 * </ul>
 */
@Getter
@Setter
@Slf4j
public class BookingWorker implements Runnable {

    // ── Constants ────────────────────────────────────────────────────────────────
    private static final Duration API_TIMEOUT   = Duration.ofSeconds(10);
    private static final int      MAX_USER_LOGS = 20;

    /** Base poll interval for ADMIN users (ms). */
    public static final long POLL_INTERVAL_ADMIN_MS  = 200L;
    /** Base poll interval for normal USER (ms). */
    public static final long POLL_INTERVAL_USER_MS   = 600L;

    // ── Lifecycle flags ───────────────────────────────────────────────────────────
    private volatile boolean isPaused = false;
    private volatile boolean isStop   = false;

    // ── Sleep / shuffle controls — updated externally by SessionManagerScheduler ──
    /** Base poll interval set at construction time based on user role (ms). */
    private volatile long    pollIntervalMs;
    /** When true the worker sleeps shuffleSleepMs instead of pollIntervalMs. */
    private volatile boolean shuffleMode    = false;
    /** Random sleep used when shuffleMode is active (ms, set by scheduler). */
    private volatile long    shuffleSleepMs = 10_000L;

    // ── Live date → times map (mutated as slots are booked) ──────────────────────
    private final LinkedHashMap<String, LinkedHashSet<TimesList>> dateAndTime;

    // ── Auth — updated externally by the scheduler via Lombok setters ─────────────
    private volatile String              accessToken;
    private volatile String              refreshToken;
    private volatile java.time.LocalDateTime lastRefreshedAt;

    // ── Infrastructure ────────────────────────────────────────────────────────────
    private       UserHeaderModel      headers;
    private final WebClientServices    webClientServices;

    // ── User-visible logs ─────────────────────────────────────────────────────────
    private final List<Logs> inMemoryUserLogs = Collections.synchronizedList(new ArrayList<>());

    // ── Session stats ─────────────────────────────────────────────────────────────
    private long bookedSlotsInSession = 0L;

    // ── Constructor ───────────────────────────────────────────────────────────────

    /**
     * @param isAdmin  {@code true} for ADMIN/MAINTAINER users (200 ms poll interval);
     *                 {@code false} for normal USER (600 ms poll interval).
     */
    public BookingWorker(UserHeaderModel headers,
                         String accessToken,
                         String refreshToken,
                         WebClientServices webClientServices,
                         UserRequestedDateAndTime dateAndTime,
                         boolean isAdmin) {
        this.headers           = headers;
        this.accessToken       = accessToken;
        this.refreshToken      = refreshToken;
        this.webClientServices = webClientServices;
        this.pollIntervalMs = isAdmin ? POLL_INTERVAL_ADMIN_MS : POLL_INTERVAL_USER_MS;

        // Auto-enable shuffle mode if starting during the night window (00:00 to 05:00 IST)
        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        if (now.getHour() >= 0 && now.getHour() < 5) {
            this.shuffleSleepMs = 10_000L + (long) (Math.random() * 10_000L); // Random 10s to 20s
            this.shuffleMode = true;
        } else {
            this.shuffleMode = false;
        }

        // Initialize pinned log correctly
        addUserLog(String.format("No issue found, working fine. | Access: %s | Refresh: %s",
                maskToken(accessToken), maskToken(refreshToken)));

        // Defensive copy so callers cannot mutate our live state
        this.dateAndTime = dateAndTime != null && dateAndTime.getDateAndTime() != null
                ? new LinkedHashMap<>(dateAndTime.getDateAndTime())
                : new LinkedHashMap<>();
    }

    // ── Lifecycle controls ────────────────────────────────────────────────────────

    public boolean pause()  { this.isPaused = true;  return true; }
    public boolean resume() { this.isPaused = false; return true; }
    public boolean stop()   { this.isStop   = true;  return true; }

    // ── Shuffle controls (called by SessionManagerScheduler) ─────────────────────

    /** Enable shuffle mode: worker will sleep {@code sleepMs} ms per cycle. */
    public void enableShuffle(long sleepMs) {
        this.shuffleSleepMs = sleepMs;
        this.shuffleMode    = true;
        addUserLog("Lazy mode turned on, it will automatically turn off at 5 AM.");
    }

    /** Disable shuffle mode: worker reverts to its role-based pollIntervalMs. */
    public void disableShuffle() {
        if (this.shuffleMode) {
            this.shuffleMode = false;
            addUserLog("Lazy mode turned off, resuming normal speed.");
        }
    }

    // ── Main loop ─────────────────────────────────────────────────────────────────

    @Override
    public void run() {
        // Terminal log — printed exactly ONCE at startup
        log.info("[BookingWorker] Started for user={} store={} dates={} times={}",
                headers.getEmployeeName(),
                headers.getSiteId(),
                new ArrayList<>(dateAndTime.keySet()),
                dateAndTime.values());

        try {
            while (!isStop) {

                if (isPaused) {
                    try { Thread.sleep(3_000); } catch (InterruptedException e) { break; }
                    continue;
                }

                // Auto-stop when all dates are done
                if (dateAndTime.isEmpty()) {
                    log.info("[BookingWorker] All dates completed for user={} store={}. Stopping.",
                            headers.getEmployeeName(), headers.getSiteId());
                    isStop = true;
                    continue;
                }

                try {
                    fetchAndBookAllDatesAsync();
                    // Use shuffle sleep when active, otherwise use role-based base interval
                    long sleepMs = shuffleMode ? shuffleSleepMs : pollIntervalMs;
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable t) {
                    log.error("[BookingWorker] Unexpected error for user={}: {}",
                            headers.getEmployeeName(), t.toString());
                    break;
                }
            }
        } finally {
            log.info("[BookingWorker] Worker finished for user={} store={}.",
                    headers.getEmployeeName(), headers.getSiteId());
        }
    }

    // ── Async parallel fetch + book ───────────────────────────────────────────────

    /**
     * Fires one async request per pending date simultaneously.
     * All dates are sent without waiting for the previous one to complete.
     * Waits for all to finish before the next poll cycle begins.
     */
    private void fetchAndBookAllDatesAsync() {
        List<String> currentDates;
        synchronized (dateAndTime) {
            currentDates = new ArrayList<>(dateAndTime.keySet());
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String endDateUtc : currentDates) {
            String startDateUtc = DateToUtc.getPrevDateToUtc(endDateUtc);

            FetchSlotsRequest request = FetchSlotsRequest.builder()
                    .endDate(endDateUtc)
                    .startDate(startDateUtc)
                    .locationInfo(FetchSlotsRequest.Location.builder()
                            .xLat(Double.parseDouble(headers.getXLat().trim()))
                            .xLong(Double.parseDouble(headers.getXLong().trim()))
                            .build())
                    .build();

            futures.add(CompletableFuture.runAsync(() -> processOneDate(endDateUtc, request)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Handles the full fetch → filter → book → cleanup flow for a single date.
     * Errors are swallowed silently — the next cycle will retry automatically.
     */
    private void processOneDate(String endDateUtc, FetchSlotsRequest request) {
        try {
            // ── Fetch slots ──────────────────────────────────────────────────────
            FetchSlotsResponse response = webClientServices.getSlotsDetails(headers, request, this.accessToken);

            if (response == null || !response.isSuccess()) {
                return;
            }

            // ── Collect requested times for this date ────────────────────────────
            LinkedHashSet<TimesList> timesForDate;
            synchronized (dateAndTime) {
                timesForDate = dateAndTime.get(endDateUtc);
            }

            List<String> requestedTimes = collectTimes(timesForDate);

            // ── Filter matching slot IDs ─────────────────────────────────────────
            Map<String, String> slotIdToTimeKey = filterSlots(response, requestedTimes);

            if (slotIdToTimeKey.isEmpty()) {
                return;
            }

            // ── Book each slot independently in parallel ──────────────────────────
            // If one slot was already taken, only that one fails —
            // the rest are still attempted and can succeed.
            List<CompletableFuture<Void>> bookingJobs = new ArrayList<>();

            for (Map.Entry<String, String> entry : slotIdToTimeKey.entrySet()) {
                String slotId  = entry.getKey();
                String timeKey = entry.getValue();

                CompletableFuture<Void> job = CompletableFuture.runAsync(() -> {
                    try {
                        ResponseEntity<GlobalRespons> resp = webClientServices
                                .bookSlots(
                                        headers,
                                        BookSlotsRequest.builder()
                                                .slotIds(List.of(slotId))
                                                .build(),
                                        timeKey,
                                        this.accessToken)
                                .block(API_TIMEOUT);

                        if (resp == null
                                || !resp.getStatusCode().is2xxSuccessful()
                                || resp.getBody() == null
                                || !resp.getBody().isSuccess()) {
                            // Slot unavailable or already taken — skip silently
                            return;
                        }

                        // ── Single slot booked successfully ──────────────────────
                        bookedSlotsInSession++;

                        // Terminal log — one line per slot
                        log.info("[BookingWorker] BOOKED — user={} store={} date={} slot={} time={}",
                                headers.getEmployeeName(), headers.getSiteId(),
                                endDateUtc, slotId, timeKey);

                        // User log — only on success
                        addUserLog("Slot booked for date " + endDateUtc + " | time: " + timeKey);

                        // Remove only this slot's time from the date's queue
                        removeBookedTimesFromDate(endDateUtc, Map.of(slotId, timeKey));

                    } catch (Throwable e) {
                        log.error("[BookingWorker] Failed to book slotId={} date={} user={}: {}",
                                slotId, endDateUtc, headers.getEmployeeName(), e.toString());
                    }
                });

                bookingJobs.add(job);
            }

            // Wait for all per-slot booking attempts to complete
            CompletableFuture.allOf(bookingJobs.toArray(new CompletableFuture[0])).join();

        } catch (Throwable e) {
            log.error("[BookingWorker] Error on date={} for user={}: {}",
                    endDateUtc, headers.getEmployeeName(), e.toString());
        }
    }

    // ── Time/date removal ─────────────────────────────────────────────────────────

    /**
     * After a successful booking removes the booked time keys from the date's
     * time-list. If the list becomes empty the date is also removed, eventually
     * triggering auto-stop when all dates are gone.
     */
    private void removeBookedTimesFromDate(String endDateUtc, Map<String, String> bookedSlotMap) {
        synchronized (dateAndTime) {
            LinkedHashSet<TimesList> timesSet = dateAndTime.get(endDateUtc);

            if (timesSet == null || timesSet.isEmpty()) {
                dateAndTime.remove(endDateUtc);
                return;
            }

            for (String bookedTimeKey : bookedSlotMap.values()) {
                for (TimesList tl : timesSet) {
                    if (tl.getTimes() != null) {
                        tl.getTimes().remove(bookedTimeKey); // Remove only this specific time
                    }
                }
                // Only remove the TimesList object if all its times are gone
                timesSet.removeIf(tl -> tl.getTimes() == null || tl.getTimes().isEmpty());
            }

            if (timesSet.isEmpty()) {
                dateAndTime.remove(endDateUtc);
                log.info("[BookingWorker] All slots booked for date={}. Date removed from queue.", endDateUtc);
            }
        }
    }

    // ── Slot filtering ────────────────────────────────────────────────────────────

    /**
     * Finds the user's store in the API response, keeps only unbooked+allowed slots,
     * then matches each against the user's requested times.
     *
     * @return slotId → IST time-key map; empty if nothing matched
     */
    private Map<String, String> filterSlots(FetchSlotsResponse response, List<String> requestedTimes) {
        if (response == null || response.getData() == null || response.getData().getStores() == null) {
            return Collections.emptyMap();
        }

        String userStoreId = headers.getSiteId();

        // Step 1 — locate the user's store
        FetchSlotsResponse.Store matchedStore = null;
        for (FetchSlotsResponse.Store store : response.getData().getStores()) {
            if (userStoreId != null && userStoreId.equals(store.getId())) {
                matchedStore = store;
                break;
            }
        }

        if (matchedStore == null || matchedStore.getSlots() == null || matchedStore.getSlots().isEmpty()) {
            return Collections.emptyMap();
        }

        // Step 2 — keep unbooked + booking-allowed slots
        List<FetchSlotsResponse.Slot> available = new ArrayList<>();
        for (FetchSlotsResponse.Slot slot : matchedStore.getSlots()) {
            boolean allowed = slot.getBookingEligibility() != null && slot.getBookingEligibility().isAllowed();
            if (!slot.isBooked() && allowed) {
                available.add(slot);
            }
        }

        if (available.isEmpty()) {
            return Collections.emptyMap();
        }

        // Step 3 — "all" mode: return every available slot
        boolean acceptAll = requestedTimes == null || requestedTimes.isEmpty()
                || (requestedTimes.size() == 1 && "all".equalsIgnoreCase(requestedTimes.get(0)));

        if (acceptAll) {
            Map<String, String> all = new LinkedHashMap<>();
            for (FetchSlotsResponse.Slot slot : available) {
                all.put(String.valueOf(slot.getId()),
                        DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime()));
            }
            return all;
        }

        // Step 4 — match by preferred time windows, preserving order
        Map<String, String> matched = new LinkedHashMap<>();
        for (String preferred : requestedTimes) {
            for (FetchSlotsResponse.Slot slot : available) {
                if (DateToUtc.isTimeMatch(preferred, slot.getStartTime(), slot.getEndTime())) {
                    matched.put(String.valueOf(slot.getId()), preferred); // Use the exact string the user requested
                }
            }
        }

        return matched;
    }

    // ── User logs ─────────────────────────────────────────────────────────────────

    private String maskToken(String token) {
        if (token == null) return "null";
        if (token.length() <= 15) return "***";
        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }

    /**
     * Returns the user-visible log list. The pinned entry is dynamically generated
     * to show the current masked tokens and prepended first.
     * Called by BookingServices to serve the /logs endpoint.
     */
    public List<Logs> getLogs() {
        synchronized (inMemoryUserLogs) {
            List<Logs> result = new ArrayList<>();
            String pinnedLog = String.format("No issue found, working fine. | Access: %s | Refresh: %s",
                    maskToken(this.accessToken), maskToken(this.refreshToken));
            result.add(Logs.builder().logs(List.of(pinnedLog)).build());
            result.addAll(inMemoryUserLogs);
            return result;
        }
    }

    /**
     * Appends a user-visible log entry.
     * When the cap (20) is reached all old entries are cleared first — the
     * pinned entry is always prepended dynamically so it is never lost.
     */
    private void addUserLog(String message) {
        synchronized (inMemoryUserLogs) {
            if (inMemoryUserLogs.size() >= MAX_USER_LOGS) {
                inMemoryUserLogs.clear();
            }
            inMemoryUserLogs.add(Logs.builder().logs(List.of(message)).build());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Flattens a {@code LinkedHashSet<TimesList>} into a single {@code List<String>}. */
    private static List<String> collectTimes(LinkedHashSet<TimesList> timesSet) {
        if (timesSet == null || timesSet.isEmpty()) return Collections.emptyList();
        List<String> flat = new ArrayList<>();
        for (TimesList tl : timesSet) {
            if (tl.getTimes() != null) flat.addAll(tl.getTimes());
        }
        return flat;
    }

    // ── External date/time mutation (called by BookingServices API) ───────────────

    /** Removes an entire date from the queue. */
    public boolean removeOneDateFromList(String date) {
        synchronized (dateAndTime) {
            return dateAndTime.remove(date) != null;
        }
    }

    /** Removes a specific time from a specific date. Removes the date if no times remain. */
    public boolean removeOneTimeFromDate(String date, String time) {
        synchronized (dateAndTime) {
            LinkedHashSet<TimesList> timesSet = dateAndTime.get(date);
            if (timesSet == null) return false;
            boolean removed = timesSet.removeIf(tl -> tl.getTimes() != null && tl.getTimes().remove(time));
            if (timesSet.isEmpty()) dateAndTime.remove(date);
            return removed;
        }
    }

    /** Returns all pending dates (for display / API). */
    public List<String> getDates() {
        synchronized (dateAndTime) {
            return new ArrayList<>(dateAndTime.keySet());
        }
    }

    /** Returns all pending times flattened across all dates (for display / API). */
    public List<String> getTimes() {
        synchronized (dateAndTime) {
            List<String> all = new ArrayList<>();
            for (LinkedHashSet<TimesList> timesSet : dateAndTime.values()) {
                all.addAll(collectTimes(timesSet));
            }
            return all;
        }
    }
}
