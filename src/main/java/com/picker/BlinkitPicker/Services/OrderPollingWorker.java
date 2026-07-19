package com.picker.BlinkitPicker.Services;

import java.util.ArrayList;
import java.util.List;
import com.picker.BlinkitPicker.Dto.respons.CustomOrderResponse;
import com.picker.BlinkitPicker.Dto.respons.SinglePollingResponse;
import com.picker.BlinkitPicker.Dto.respons.PickListResponse;
import com.picker.BlinkitPicker.Dto.respons.PicklistItemResponse;
import com.picker.BlinkitPicker.Dto.respons.CognitoRefreshTokenRespons;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.GenerateCookie;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

public class OrderPollingWorker implements Runnable {

    private final String userId;
    private final UserModel user;
    private final WebClientPick webClientPick;
    private final WebClientServices webClientServices;
    private final UserRepo userRepo;
    private volatile boolean isStop = false;
    private volatile CustomOrderResponse latestOrderResponse;

    private String jwtToken;
    private String refreshToken;

    public OrderPollingWorker(String userId, UserModel user, WebClientPick webClientPick, WebClientServices webClientServices, UserRepo userRepo) {
        this.userId = userId;
        this.user = user;
        this.webClientPick = webClientPick;
        this.webClientServices = webClientServices;
        this.userRepo = userRepo;
        this.jwtToken = user.getUserHeaders().getAuthorization();
        this.refreshToken = user.getRefreshToken();
        this.latestOrderResponse = CustomOrderResponse.builder()
                .success(false)
                .items(new ArrayList<>())
                .build();
    }

    public void stop() {
        this.isStop = true;
    }

    public CustomOrderResponse getLatestOrderResponse() {
        return this.latestOrderResponse;
    }

    @Override
    public void run() {
        System.out.println("[OrderPollingWorker - " + userId + "] Thread started successfully!");
        while (!this.isStop) {
            try {
                pollTasks();
                Thread.sleep(5000); // Poll every 5 seconds
            } catch (InterruptedException e) {
                break;
            } catch (Throwable t) {
                System.err.println("[OrderPollingWorker - ERROR] Thread crashed: " + t.toString());
                break;
            }
        }
        System.out.println("[OrderPollingWorker - " + userId + "] Thread stopped.");
    }

    public CustomOrderResponse pollTasks() {
        // Dynamic cookie/session generation on every poll cycle
        String cfbm = GenerateCookie.generateCfBmCookie();
        String requestId = GenerateCookie.generateRequestId();
        String sessionToken = GenerateCookie.generateSessionToken();
        String httpSessionToken = sessionToken;

        String role = user.getRole() != null ? user.getRole().toString() : "PICKER";
        String employeeId = user.getUserHeaders().getEmployeeId();
        String userAgent = user.getUserHeaders().getUserAgent();
        String xDeviceId = user.getUserHeaders().getXDeviceId();
        String siteId = user.getUserHeaders().getSiteId();

        try {
            SinglePollingResponse response = blockWithTokenRefresh(() -> webClientPick.pollPendingTasks(
                    cfbm, requestId, jwtToken, userAgent, xDeviceId, 
                    role, employeeId, httpSessionToken, sessionToken, siteId
            ));

            if (response != null && response.isSuccess() && response.getData() != null && response.getData().getTasks() != null) {
                for (SinglePollingResponse.TaskInfo task : response.getData().getTasks()) {
                    if ("PICKING".equalsIgnoreCase(task.getTaskType()) && task.getTaskDetails() != null) {
                        PickListResponse details = task.getTaskDetails();
                        List<CustomOrderResponse.ItemInfo> customItems = new ArrayList<>();
                        
                        if (details.getItemList() != null) {
                            for (PicklistItemResponse item : details.getItemList()) {
                                customItems.add(CustomOrderResponse.ItemInfo.builder()
                                        .productName(item.getItemName())
                                        .quantity(item.getRequiredQuantity())
                                        .productImage(item.getItemImageUrl())
                                        .build());
                            }
                        }

                        CustomOrderResponse newResult = CustomOrderResponse.builder()
                                .success(true)
                                .activityId(details.getActivityId())
                                .items(customItems)
                                .build();

                        this.latestOrderResponse = newResult;
                        return newResult;
                    }
                }
            }
        } catch (Throwable e) {
            System.err.println("[OrderPollingWorker - " + userId + "] Error polling tasks: " + e.getMessage());
        }
        return this.latestOrderResponse;
    }

    private <T> T blockWithTokenRefresh(java.util.function.Supplier<Mono<T>> apiCall) {
        try {
            return apiCall.get().block();
        } catch (WebClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            if (statusCode != 401 && statusCode != 403) {
                throw e;
            }

            System.out.println("[OrderPollingWorker - " + userId + "] API returned HTTP " + statusCode + ". Refreshing JWT token.");

            if (!refreshJwtToken()) {
                throw e;
            }

            System.out.println("[OrderPollingWorker - " + userId + "] JWT token refreshed. Retrying API call.");
            return apiCall.get().block();
        }
    }

    private boolean refreshJwtToken() {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                System.out.println("[OrderPollingWorker - " + userId + "] Cannot refresh JWT token: refresh token is missing.");
                return false;
            }

            CognitoRefreshTokenRespons response = webClientServices.refreshToken(refreshToken);
            if (response == null || response.getAuthenticationResult() == null
                    || response.getAuthenticationResult().getIdToken() == null
                    || response.getAuthenticationResult().getIdToken().isBlank()) {
                System.out.println("[OrderPollingWorker - " + userId + "] Cannot refresh JWT token: Cognito response has no IdToken.");
                return false;
            }

            String freshToken = response.getAuthenticationResult().getIdToken();
            this.jwtToken = freshToken;
            user.setJwt(freshToken);
            if (user.getUserHeaders() != null) {
                user.getUserHeaders().setAuthorization(freshToken);
            }

            try {
                userRepo.save(user);
            } catch (Exception dbEx) {
                System.err.println("[OrderPollingWorker - " + userId + "] Warning: Failed to save refreshed user token to DB: " + dbEx.getMessage());
            }

            return true;
        } catch (Throwable e) {
            System.err.println("[OrderPollingWorker - " + userId + "] JWT refresh failed: " + e.toString());
            return false;
        }
    }
}
