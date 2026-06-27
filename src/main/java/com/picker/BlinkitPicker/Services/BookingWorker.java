package com.picker.BlinkitPicker.Services;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;


import com.picker.BlinkitPicker.Dto.FetchSlotsRequest;
import com.picker.BlinkitPicker.Dto.FetchSlotsResponse;
import com.picker.BlinkitPicker.Dto.GlobalRespons;
import com.picker.BlinkitPicker.Dto.Logs;
import com.picker.BlinkitPicker.Dto.Internal.BookSlotsRequest;
import com.picker.BlinkitPicker.Enums.RoleEnum;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Util.DateToUtc;
import com.picker.BlinkitPicker.Util.GenerateCookie;

import reactor.core.publisher.Mono;

public class BookingWorker implements Runnable {

    private String userId;
    private List<String> dates;
    private Boolean isPaused = false;
    private Boolean isStop = false;

    private Boolean isAdmin;

    private final UserModel user;

    private WebClientServices webClientServices;

    private ConcurrentHashMap<String, Logs> logs = new ConcurrentHashMap<>();

    private String jwtToken;
    private String refreshToken;

    public BookingWorker(String userId, List<String> dates, UserModel user, WebClientServices webClientServices) {
        this.userId = userId;
        this.dates = dates;
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
            this.jwtToken = user.getUserHeaders().getAuthorization();
            this.refreshToken = user.getRefreshToken();
            String storeId = user.getUserHeaders().getSiteId();

            this.isAdmin = user.getRole().equals(RoleEnum.ADMIN);

            FetchSlotsRequest request = FetchSlotsRequest.builder()
                    .endDate(DateToUtc.getDateToUtc(dates.get(i)))
                    .startDate(DateToUtc.getPrevDateToUtc(dates.get(i)))
                    .locationInfo(FetchSlotsRequest.Location.builder()
                            .xLat(user.getUserHeaders().getXLat())
                            .xLong(user.getUserHeaders().getXLong())
                            .build())
                    .build();

            try {
                Mono<FetchSlotsResponse> response = webClientServices.getSlotsDetails(cfbm, requestId, jwtToken,
                        request,
                        storeId);

                List<String> slotIds = filterSlotId(response.block());

                if (!slotIds.isEmpty()) {

                    cfbm = GenerateCookie.generateCfBmCookie();
                    requestId = GenerateCookie.generateRequestId();

                    Mono<GlobalRespons> responseBooking = webClientServices.bookSlots(cfbm, requestId, jwtToken,
                            BookSlotsRequest.builder()
                                    .slotIds(slotIds)
                                    .build(),
                            storeId, user);

                    if (responseBooking.block().isSuccess()) {
                        logs.put(userId, Logs.builder()
                                .logs(List.of("Slot booked successfully for " + slotIds + " on date " + dates.get(i)))
                                .build());

                        continue;
                    } else {

                        for (int j = 0; j < slotIds.size(); j++) {

                            List<String> singleSlot = new ArrayList<>();
                            singleSlot.add(slotIds.get(j));

                            cfbm = GenerateCookie.generateCfBmCookie();
                            requestId = GenerateCookie.generateRequestId();

                            Mono<GlobalRespons> responseBookingRetry = webClientServices.bookSlots(cfbm, requestId,
                                    jwtToken,
                                    BookSlotsRequest.builder()
                                            .slotIds(singleSlot)
                                            .build(),
                                    storeId, user);

                            if (responseBookingRetry.block().isSuccess()) {
                                logs.put(userId, Logs.builder()
                                        .logs(List.of("Slot booked successfully for " + slotIds.get(j) + " on date "
                                                + dates.get(i)))
                                        .build());
                                continue;
                            } else {
                                logs.put(userId, Logs.builder()

                                        .logs(List.of("Failed to book slot for " + slotIds.get(j) + " on date "
                                                + dates.get(i)))
                                        .build());
                            }
                        }
                    }

                } else {

                    logs.put(userId, Logs.builder()
                            .logs(List.of("No slots available for " + storeId + " on date " + dates.get(i)))
                            .build());
                }

            } catch (Exception e) {
                logs.put(userId, Logs.builder()
                        .logs(List.of("Error in fetching slots for " + storeId + " on date " + dates.get(i)))
                        .build());
                e.printStackTrace();
            }
        }
    }

    private List<String> filterSlotId(FetchSlotsResponse response) {
        if (response == null || response.getData() == null || response.getData().getStores() == null) {
            return Collections.emptyList();
        }

        String userStoreId = user.getUserHeaders().getSiteId();

        for (FetchSlotsResponse.Store store : response.getData().getStores()) {
            if (userStoreId != null && userStoreId.equals(store.getId())) {
                if (store.getSlots() != null) {
                    List<String> slotIds = new ArrayList<>();
                    for (FetchSlotsResponse.Slot slot : store.getSlots()) {
                        slotIds.add(String.valueOf(slot.getId()));
                    }
                    return slotIds;
                }
                break;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public void run() {
        while (!this.isStop) {

            if (this.isPaused) {
                try {

                    Thread.sleep(50000);

                } catch (InterruptedException e) {

                    break;
                }
                continue;
            }

            fecthSlots();

            try {
                if (isAdmin) {
                    Thread.sleep(100);
                } else {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
