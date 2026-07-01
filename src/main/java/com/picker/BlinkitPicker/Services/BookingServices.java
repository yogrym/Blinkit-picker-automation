package com.picker.BlinkitPicker.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.BookingRequest;
import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Dto.Logs;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.SessionIdGenerator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
public class BookingServices {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final ConcurrentHashMap<String, WorkerList> workerMap = new ConcurrentHashMap<>();

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private WebClientServices webClientServices;

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

        BookingWorker worker = new BookingWorker(userId.toString(), dates, times, user, webClientServices);

        String firstDate = dates != null && !dates.isEmpty() ? dates.get(0) : null;
        String lastDate = dates != null && !dates.isEmpty() ? dates.get(dates.size() - 1) : null;

        if (workerMap.containsKey(userId.toString())) {

            workerMap.get(userId.toString()).addWorker(sessionId, worker);
            workerMap.get(userId.toString()).addBookingData(sessionId, firstDate, lastDate);
        } else {

            WorkerList userWorkers = new WorkerList();
            userWorkers.addWorker(sessionId, worker);
            userWorkers.addBookingData(sessionId, firstDate, lastDate);

            workerMap.put(userId.toString(), userWorkers);
        }

        executor.submit(worker);

        return new WorkerList.BookingData(sessionId, firstDate, lastDate);
    }

    public List<WorkerList.BookingData> getBookingData(String token) {
        Long userId = jwtServices.extractUserId(token);
        if (workerMap.containsKey(userId.toString())) {
            return workerMap.get(userId.toString()).getAllBookingData();
        }
        return Collections.emptyList();
    }

    public String stopBooking(String token, String sessionId) {
        Long userId = jwtServices.extractUserId(token);
        if (workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                worker.stop();
                userWorkers.removeWorker(sessionId);
                return "Stopped " + sessionId;
            }
        }
        return "Session not found";
    }

    public String pauseBooking(String token, String sessionId) {
        Long userId = jwtServices.extractUserId(token);
        if (workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                worker.pause();
                return "Paused " + sessionId;
            }
        }
        return "Session not found";
    }

    public String resumeBooking(String token, String sessionId) {
        Long userId = jwtServices.extractUserId(token);
        if (workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            BookingWorker worker = userWorkers.getWorker(sessionId);
            if (worker != null) {
                worker.resume();
                return "Resumed " + sessionId;
            }
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

    public List<Logs> getSessionLogs(String token, String sessionId) {
        Long userId = jwtServices.extractUserId(token);
        if (userId != null && workerMap.containsKey(userId.toString())) {
            WorkerList userWorkers = workerMap.get(userId.toString());
            if (userWorkers != null) {
                BookingWorker worker = userWorkers.getWorker(sessionId);
                if (worker != null) {
                    return worker.getLogs();
                }
            }
        }
        return Collections.emptyList();
    }

}
