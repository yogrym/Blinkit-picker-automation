package com.picker.BlinkitPicker.Services.Worker;

import java.time.Duration;
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
import com.picker.BlinkitPicker.Services.WebClientServices;
import com.picker.BlinkitPicker.Util.DateToUtc;
import com.picker.BlinkitPicker.Util.GenerateCookie;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@Getter
@Setter
@Slf4j
public class BookingWorker implements Runnable {
    
    private static final Duration API_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_IN_MEMORY_LOGS_SIZE = 10;
    private static final Logger logger =  LoggerFactory.getLogger(BookingWorker.class);
    private List<String> dates;
    private List<String> times;
    private volatile boolean isPaused = false;
    private volatile boolean isStop = false;

    private boolean isAdmin = false;

    private final UserModel user;
    private UserHeaderModel headers;
    private final UserRepo userRepo;

    private WebClientServices webClientServices;

    private final List<Logs> inMemoryUserLogs = Collections.synchronizedList(new ArrayList<>());
    private long bookedSlotsInSession = 0L;
    private boolean bookedSlotsSaved = false;

    private String accessToken;
    private String refreshToken;
    private volatile java.time.LocalDateTime lastRefreshedAt;

    public BookingWorker(String userId, List<String> dates, List<String> times, UserModel user,
            WebClientServices webClientServices, UserRepo userRepo) {
        this.dates = dates;
        this.times = times;
        this.user = user;
        this.userRepo = userRepo;
        this.headers = user.getUserHeaders();
        this.accessToken = user.getUserHeaders().getAccessToken();
        this.refreshToken = user.getUserHeaders().getRefreshToken();
        this.webClientServices = webClientServices;
    }

    public void saveUserHeaders() {
        if (this.user != null && this.headers != null) {
            Long id = this.user.getId() != null ? this.user.getId() : this.headers.getUserId();
            if (id != null) {
                UserModel latestUser = this.userRepo.findById(id).orElse(this.user);
                latestUser.setUserHeaders(this.headers);
                this.userRepo.save(latestUser);
                this.user.setUserHeaders(this.headers);
                this.user.setTotalBookedSlots(latestUser.getTotalBookedSlots());
            }
        }
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
            
            
            this.isAdmin = user.getRole().equals(RoleEnum.ADMIN) || user.getRole().equals(RoleEnum.MAINTAINER);

         
            String endDateUtc = DateToUtc.getDateToUtc(dates.get(i));
            String startDateUtc = DateToUtc.getPrevDateToUtc(endDateUtc);


            FetchSlotsRequest request = FetchSlotsRequest.builder()
                    .endDate(endDateUtc)
                    .startDate(startDateUtc)
                    .locationInfo(FetchSlotsRequest.Location.builder()
                            .xLat(Double.parseDouble(headers.getXLat().trim()))
                            .xLong(Double.parseDouble(headers.getXLong().trim()))
                            .build())
                    .build();

           logger.info("Booking has been started for {}/ selected store id is {}", headers.getEmployeeName(), headers.getSiteId());
           addLog("Booking started for store " + headers.getSiteId() + ".");

            try {

                ResponseEntity<FetchSlotsResponse> response = blockWithTokenRefresh(
                        "fetch slots",
                   () -> webClientServices.getSlotsDetails ( headers, request
                ),headers.getRefreshToken(),headers);
                                
                logger.info("Slots fetched for {}. Selected store id is {}.", headers.getEmployeeName() , headers.getSiteId());
                addLog("Slot list fetched for store " + headers.getSiteId() + " on date " + dates.get(i) + ".");

                FetchSlotsResponse responseBody = response != null ? response.getBody() : null;
                if (responseBody == null || !responseBody.isSuccess()) {
                    
                    logger.error("{} slots fetch failed for selected store id {}. Error code: {}", 
                            headers.getEmployeeName(), headers.getSiteId(), responseBody != null ? responseBody.getErrorCode() : "null");
                    addLog("Unable to fetch slots for store " + headers.getSiteId() + ".");
                    continue;
                }
               

            
                Map<String, String> slotIdToTime = filterSlotId(responseBody, times);
                List<String> slotIds = new ArrayList<>(slotIdToTime.keySet());

                if (!slotIds.isEmpty()) {
                    String timesLog = slotIdToTime.toString();

                    ResponseEntity<GlobalRespons> bookingResponse = blockWithTokenRefresh(
                            "book slots",
                            () -> webClientServices.bookSlots(
                                   headers,
                                   BookSlotsRequest.builder().slotIds(slotIds).build(),
                                    timesLog),headers.getRefreshToken(),headers);

                    if (bookingResponse.getStatusCode().is2xxSuccessful() 
                            && bookingResponse.getBody().isSuccess()) {

                        logger.info(" SUCCESS! Booked slots: {}/for user : {}/ store id: {}" + timesLog
                                + " on date " + dates.get(i), slotIds.size(), headers.getEmployeeName(), headers.getSiteId());
                        addLog("Successfully booked " + slotIds.size() + " slot(s) for " + dates.get(i) + ".");
                        incrementBookedSlots(slotIds.size());
                        continue;
                    } 
                    
                    
                    else {

                    int statusCode = bookingResponse.getStatusCode().value();

                      logger.error(
            "{} failed to book slots for employee: {}, store id: {}, date: {}",
                     statusCode,
                     headers.getEmployeeName(),
                     headers.getSiteId(),
                     dates.get(i));           
                    addLog("Booking failed for store " + headers.getSiteId() + " on date " + dates.get(i) + ".");
                    
                    continue;
                    }   

                }

            } catch (Throwable e) {
                logger.error("[BookingWorker - " + headers.getEmployeeName() + "] Slot fetch/booking failed: {}", e.toString());
                addLog("Slot fetching or booking failed for date " + dates.get(i) + ".");
            }
        }
    }

    private <T> T blockWithTokenRefresh(String operationName, Supplier<Mono<T>> apiCall,String refreshToken,UserHeaderModel headers) {
        try {
            return apiCall.get().block(API_TIMEOUT);
        } catch (WebClientResponseException e) {
            if (!isUnauthorizedOrForbidden(e)) {
                throw e;
            }

            logger.error("[{}]] " + operationName + " returned HTTP "
                    + e.getStatusCode().value() + ". Refreshing AccessToken.", headers.getEmployeeName());

            addLog("Session expired while processing your booking. Refreshing the session.");

            if (!refreshAccessToken(refreshToken,headers)) {
                this.pause();
                addLog("Automatic session refresh failed. Booking is now paused. Please login again to resume.");
                throw e;
            }

            logger.info("[BookingWorker - " + headers.getEmployeeName() + "] AccessToken refreshed. Retrying " + operationName + ".");
            addLog("Session refreshed. Retrying the request.");
            return apiCall.get().block();
        }
    }

    private boolean isUnauthorizedOrForbidden(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        return statusCode == 401 || statusCode == 403;
    }

    public boolean forceAccessTokenRefresh() {
        return refreshAccessToken(this.refreshToken, this.headers);
    }

    /**
     * Called by the scheduler to refresh tokens directly inside the live booking thread.
     * Updates in-thread accessToken, refreshToken, headers, and lastRefreshedAt.
     * The scheduler is responsible for persisting the new tokens to DB.
     * Never throws — returns false on any failure.
     */
    public boolean refreshTokensFromScheduler(WebClientServices webClientServices) {
        try {
            if (this.headers == null || this.refreshToken == null) {
                logger.warn("[BookingWorker] Cannot refresh: headers or refreshToken is null for user {}",
                        this.headers != null ? this.headers.getEmployeeName() : "unknown");
                return false;
            }

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("refresh_token", this.refreshToken);

            CognitoRefreshTokenRespons response = webClientServices.refreshToken(formData, this.headers);

            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                logger.warn("[BookingWorker] Scheduler token refresh failed for user {}",
                        this.headers.getEmployeeName());
                addLog("Scheduled session refresh failed.");
                return false;
            }

            String newAccessToken  = response.getAccessToken();
            String newRefreshToken = response.getRefreshToken();

            if (newAccessToken == null || newRefreshToken == null) {
                logger.warn("[BookingWorker] Scheduler received null tokens for user {}",
                        this.headers.getEmployeeName());
                addLog("Scheduled session refresh returned empty tokens.");
                return false;
            }

            // Update in-thread state immediately — the running loop uses these next iteration
            this.accessToken     = newAccessToken;
            this.refreshToken    = newRefreshToken;
            this.headers.setAccessToken(newAccessToken);
            this.headers.setRefreshToken(newRefreshToken);
            this.lastRefreshedAt = java.time.LocalDateTime.now();

            logger.info("[BookingWorker] Tokens refreshed in-thread for user {}", this.headers.getEmployeeName());
            addLog("Your session tokens have been refreshed by the scheduler.");
            return true;

        } catch (Exception e) {
            logger.error("[BookingWorker] Error during scheduler token refresh for user {}: {}",
                    this.headers != null ? this.headers.getEmployeeName() : "unknown", e.getMessage(), e);
            addLog("Scheduled session refresh encountered an error.");
            return false;
        }
    }

    private boolean refreshAccessToken(String refreshToken, UserHeaderModel headers) {
        try {

            MultiValueMap<String,String> formData = new LinkedMultiValueMap<>();
            formData.add("refresh_token", refreshToken);

            CognitoRefreshTokenRespons response = webClientServices.refreshToken(formData,headers);

            if (!response.getSuccess()) {
                logger.error("Cannot refresh AccessToken for user {}: ", headers.getEmployeeName());
                addLog("Session refresh failed.");
                return false;
            }

            if (response.getAccessToken() != null && response.getRefreshToken()!=null) {
                headers.setAccessToken(response.getAccessToken());
                headers.setRefreshToken(response.getRefreshToken());
                this.accessToken = response.getAccessToken();
                this.refreshToken = response.getRefreshToken();
                logger.info("User session has been renewed  {}: ", headers.getEmployeeName());
                addLog("Your session has been renewed");
                return true;
            } else{
                return false;
            }

           
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            logger.error("[TOKEN-ROTATE] Blinkit responded with HTTP {} for user {}. Response body: {}",
                    e.getStatusCode().value(), headers.getEmployeeName(), responseBody);
            addLog("Session refresh failed.");
            return false;
        } catch (Throwable e) {
            logger.error("[TOKEN-ROTATE] Unexpected error refreshing token for {}: {}", headers.getEmployeeName(), e.toString());
            addLog("Session refresh failed.");
            return false;
        }
    }

   
    private Map<String, String> filterSlotId(FetchSlotsResponse response, List<String> times) {
        if (response == null || response.getData() == null || response.getData().getStores() == null) {
            return Collections.emptyMap();
        }

        String userStoreId = user.getUserHeaders() != null ? user.getUserHeaders().getSiteId() : null;
        logger.debug("Filtering slots for store {}.", userStoreId);
        addLog("Checking available slots for store " + userStoreId + ".");

        // ── Step 1: find the matching store ──────────────────────────────────────
        FetchSlotsResponse.Store matchedStore = null;
        String storeName = null;
        for (FetchSlotsResponse.Store store : response.getData().getStores()) {
            if (userStoreId != null && userStoreId.equals(store.getId())) {
                matchedStore = store;
                storeName = store.getName();
                break;
            }
        }

        if (matchedStore == null) {
            logger.debug("Currently no slots available for store " + storeName + "for user " + headers.getEmployeeName());
            addLog("No slots are currently available for store " + userStoreId + ".");
            return Collections.emptyMap();
        }

        if (matchedStore.getSlots() == null || matchedStore.getSlots().isEmpty()) {
            logger.debug("Empty slots list for store " + storeName + " for user " + headers.getEmployeeName());
            addLog("No slots are currently available for store " + userStoreId + ".");
            return Collections.emptyMap();
        }

        
        List<FetchSlotsResponse.Slot> availableSlots = new ArrayList<>();

        for (FetchSlotsResponse.Slot slot : matchedStore.getSlots()) {
            boolean isBooked = slot.isBooked();
            boolean allowed = slot.getBookingEligibility() != null && slot.getBookingEligibility().isAllowed();

            if (!isBooked && allowed) {
                availableSlots.add(slot); 
            }
        }

        if (availableSlots.isEmpty()) {
            logger.debug("No available slots after filtering for store " + storeName + " for user " + headers.getEmployeeName());
            addLog("No available slots matched the booking requirements.");
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

            logger.debug("[BookingWorker] Mode=ALL -> returning " + all.size() + " available slots.");
            addLog(all.size() + " available slot(s) found.");
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
                    logger.info("A slot matched the preferred shift time {}.", preferredKey);
                }
            }
        }

        if (matchedSlots.isEmpty()) {
            // Log what we saw vs what we wanted — mirrors bot's debug log
            List<String> seenKeys = new ArrayList<>();
            for (FetchSlotsResponse.Slot slot : availableSlots) {
                seenKeys.add(DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime()));
            }
                    logger.debug("No slots matched preferred time windows. Preferred keys: " + times
                    + " | Seen keys: " + seenKeys);
            addLog("No slots matched your preferred shift times.");
        }

        return matchedSlots;
    }

   

    public List<Logs> getLogs() {
        synchronized (inMemoryUserLogs) {
            return new ArrayList<>(inMemoryUserLogs);
        }
    }
    

    private void addLog(String  log) {
        synchronized (inMemoryUserLogs) {
            if (inMemoryUserLogs.size() >= MAX_IN_MEMORY_LOGS_SIZE) {
                inMemoryUserLogs.remove(0); // Remove oldest log
            }
            inMemoryUserLogs.add(Logs.builder().logs(List.of(log)).build());
        }
    }
    
    

    private void incrementBookedSlots(long count) {
        if (count <= 0) return;
        bookedSlotsInSession += count;
        // Persist immediately so data is never lost on JVM kill/crash
        try {
            Long id = user.getId() != null ? user.getId() : headers.getUserId();
            UserModel latestUser = userRepo.findById(id).orElse(user);
            Long currentTotal = latestUser.getTotalBookedSlots() != null ? latestUser.getTotalBookedSlots() : 0L;
            latestUser.setTotalBookedSlots(currentTotal + count);
            userRepo.save(latestUser);
            this.user.setTotalBookedSlots(currentTotal + count);
        } catch (Exception e) {
            logger.error("Failed to save booked slot count: {}", e.toString());
            addLog("Booked slot count could not be saved.");
        }
    }

   
   

    private void logError(String message) {
        logger.error(message);
    }

    public boolean removeOneDateFromList(String date) {
        dates.remove(date);
        return true;
    }

    public boolean removeOneTimeFromList(String time) {
        times.remove(time);
        return true;
    }



     @Override
    public void run() {
        try {

           logger.info("Booking has been initiated for : {}/ the seleted dates are : {}", headers.getEmployeeName(), dates);
           addLog("Booking process initiated.");

            while (!this.isStop) {

                if (this.isPaused) {
                    try {
                        Thread.sleep(50000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                

                if (dates == null || dates.isEmpty()) {
                    this.isStop = true;
                    continue;
                }

                try {

                    fecthSlots();

                    if (Boolean.TRUE.equals(isAdmin)) {
                        Thread.sleep(2000);
                    } else {
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    addLog("Booking process stopped.");
                    break;
                } catch (Throwable t) {
                    logError("[BookingWorker - " + headers.getUserId() + "] Worker stopped because of an error: " + t.toString());
                    addLog("Booking process stopped because of an error.");
                    break;
                }
            }
        } finally {
            logger.info("[BookingWorker - " + headers.getUserId() + "] Worker finished.");
            addLog("Booking process finished.");
           
        }
    }
}
