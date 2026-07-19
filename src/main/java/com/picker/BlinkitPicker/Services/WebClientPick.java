package com.picker.BlinkitPicker.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.picker.BlinkitPicker.Dto.request.PickingItemPickRequest;
import com.picker.BlinkitPicker.Dto.respons.SinglePollingResponse;
import com.picker.BlinkitPicker.Dto.respons.PickListResponse;

@Service
public class WebClientPick {

    @Autowired
    @Qualifier("blinkitWebClient")
    private WebClient blinkClient;

    @Value("${polling.tasks.url:https://storeops-api.blinkit.com/v1/worker/pending_tasks}")
    private String pollingUrl;

    @Value("${pick.item.url:https://storeops-api.blinkit.com/api/pick/}")
    private String pickUrl;

    /**
     * Polls the server for pending/assigned tasks (GET).
     */
    public Mono<SinglePollingResponse> pollPendingTasks(
            String cfBm, 
            String requestId, 
            String jwt,
            String userAgent, 
            String xDeviceId, 
            String role, 
            String employeeId,
            String httpSessionToken, 
            String sessionToken, 
            String siteId) {

        String authHeader = jwt != null && jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;

        return blinkClient.get()
                .uri(pollingUrl)
                .header("requestid", requestId)
                .header("cookie", "__cf_bm=" + cfBm)
                .header("authorization", authHeader)
                .header("user-agent", userAgent != null ? userAgent
                        : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
                .header("x-device-id", xDeviceId)
                .header("x-role", role)
                .header("x-employeeid", employeeId)
                .header("http_session_token", httpSessionToken)
                .header("session-token", sessionToken)
                .header("site-id", siteId)
                .retrieve()
                .bodyToMono(SinglePollingResponse.class)
                .doOnNext(res -> System.out.println("[WebClientPick] Successfully polled pending tasks"))
                .doOnError(e -> System.out.println("[WebClientPick - ERROR] Failed to poll pending tasks: " + e.getMessage()));
    }

    /**
     * Starts the picking activity (POST).
     */
    public Mono<PickListResponse> startPicking(
            String cfBm, 
            String requestId, 
            String jwt,
            String userAgent, 
            String xDeviceId, 
            String role, 
            String employeeId,
            String httpSessionToken, 
            String sessionToken, 
            String siteId,
            Long activityId) {

        String authHeader = jwt != null && jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;
        String startUrl = "https://storeops-api.blinkit.com/api/v3/activity/" + activityId + "/start";

        return blinkClient.post()
                .uri(startUrl)
                .header("requestid", requestId)
                .header("cookie", "__cf_bm=" + cfBm)
                .header("authorization", authHeader)
                .header("user-agent", userAgent != null ? userAgent
                        : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
                .header("x-device-id", xDeviceId)
                .header("x-role", role)
                .header("x-employeeid", employeeId)
                .header("http_session_token", httpSessionToken)
                .header("session-token", sessionToken)
                .header("site-id", siteId)
                .retrieve()
                .bodyToMono(PickListResponse.class)
                .doOnNext(res -> System.out.println("[WebClientPick] Successfully started picking activity"))
                .doOnError(e -> System.out.println("[WebClientPick - ERROR] Failed to start picking activity: " + e.getMessage()));
    }

    /**
     * Commits a picked/scanned item (POST).
     */
    public Mono<PickListResponse> pickItem(
            String cfBm, 
            String requestId, 
            String jwt,
            String userAgent, 
            String xDeviceId, 
            String role, 
            String employeeId,
            String httpSessionToken, 
            String sessionToken, 
            String siteId,
            PickingItemPickRequest pickRequest) {

        String authHeader = jwt != null && jwt.startsWith("Bearer ") ? jwt : "Bearer " + jwt;

        return blinkClient.post()
                .uri(pickUrl)
                .header("requestid", requestId)
                .header("cookie", "__cf_bm=" + cfBm)
                .header("authorization", authHeader)
                .header("user-agent", userAgent != null ? userAgent
                        : "com.blinkitstoreops/156301 (Linux; Android 10; CPH1819)")
                .header("x-device-id", xDeviceId)
                .header("x-role", role)
                .header("x-employeeid", employeeId)
                .header("http_session_token", httpSessionToken)
                .header("session-token", sessionToken)
                .header("site-id", siteId)
                .bodyValue(pickRequest)
                .retrieve()
                .bodyToMono(PickListResponse.class)
                .doOnNext(res -> System.out.println("[WebClientPick] Successfully committed item pick"))
                .doOnError(e -> System.out.println("[WebClientPick - ERROR] Failed to commit item pick: " + e.getMessage()));
    }
}
