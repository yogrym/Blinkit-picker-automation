package com.picker.BlinkitPicker.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.picker.BlinkitPicker.Dto.respons.CustomOrderResponse;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderPollingServices {

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private WebClientPick webClientPick;

    @Autowired
    private WebClientServices webClientServices;

    private final Map<String, OrderPollingWorker> activeWorkers = new ConcurrentHashMap<>();
    private final Map<String, Thread> activeThreads = new ConcurrentHashMap<>();

    public CustomOrderResponse startPolling(String token) {
        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            throw new RuntimeException("JWT token is expired or invalid");
        }

        Long userId = claims.get("userId", Long.class);
        String userIdStr = userId.toString();

        UserModel user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        OrderPollingWorker worker = activeWorkers.get(userIdStr);
        if (worker == null) {
            worker = new OrderPollingWorker(userIdStr, user, webClientPick, webClientServices, userRepo);
            activeWorkers.put(userIdStr, worker);

            Thread thread = new Thread(worker);
            thread.setName("OrderPollingWorker-" + userIdStr);
            thread.start();
            activeThreads.put(userIdStr, thread);

            // Execute an immediate synchronous poll check to retrieve results on startup
            return worker.pollTasks();
        }

        return worker.getLatestOrderResponse();
    }

    public String stopPolling(String token) {
        var claims = jwtServices.extractClaimsSafely(token);
        if (claims == null) {
            throw new RuntimeException("JWT token is expired or invalid");
        }

        Long userId = claims.get("userId", Long.class);
        String userIdStr = userId.toString();

        OrderPollingWorker worker = activeWorkers.remove(userIdStr);
        Thread thread = activeThreads.remove(userIdStr);

        if (worker != null) {
            worker.stop();
        }
        if (thread != null) {
            thread.interrupt();
        }

        return "Order polling stopped for user " + userIdStr;
    }
}
