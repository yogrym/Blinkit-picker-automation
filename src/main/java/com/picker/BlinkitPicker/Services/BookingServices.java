package com.picker.BlinkitPicker.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Dto.WorkerList.BookingData;
import com.picker.BlinkitPicker.Dto.request.BookingRequest;
import com.picker.BlinkitPicker.Dto.respons.LogsResponse;
import com.picker.BlinkitPicker.Dto.Logs;
import com.picker.BlinkitPicker.Model.BookingTaskModel;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.BookingTaskRepo;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.SessionIdGenerator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
public class BookingServices implements ApplicationRunner {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final ConcurrentHashMap<String, WorkerList> workerMap = new ConcurrentHashMap<>();

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

        BookingWorker worker = new BookingWorker(userId.toString(), dates, times, user, webClientServices, userRepo);

        String firstDate = dates != null && !dates.isEmpty() ? dates.get(0) : null;
        String lastDate = dates != null && !dates.isEmpty() ? dates.get(dates.size() - 1) : null;

        bookingTaskRepo.save(BookingTaskModel.builder()
                .sessionId(sessionId)
                .userId(userId)
                .dates(dates)
                .times(times)
                .paused(false)
                .active(true)
                .firstDate(firstDate)
                .lastDate(lastDate)
                .build());

        addWorkerToMemory(userId.toString(), sessionId, worker, false, firstDate, lastDate);

        return new WorkerList.BookingData(false, sessionId, firstDate, lastDate);
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
                bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId).ifPresent(task -> {
                    task.setActive(false);
                    bookingTaskRepo.save(task);
                });
                return "Stopped " + sessionId;
            }
        }
        if (markTaskInactive(userId, sessionId)) {
            return "Stopped " + sessionId;
        }
        return "Session not found";
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

                BookingWorker worker = new BookingWorker(
                        task.getUserId().toString(),
                        task.getDates(),
                        task.getTimes(),
                        user,
                        webClientServices,
                        userRepo);

                if (Boolean.TRUE.equals(task.getPaused())) {
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

    private boolean markTaskInactive(Long userId, String sessionId) {
        return bookingTaskRepo.findByUserIdAndSessionIdAndActiveTrue(userId, sessionId)
                .map(task -> {
                    task.setActive(false);
                    bookingTaskRepo.save(task);
                    return true;
                })
                .orElse(false);
    }

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
            token.substring(7);
        }

        Long userId = jwtServices.extractUserId(token);

        if (userId == null && date == null) {
            return false;
        }

        WorkerList userWorker = workerMap.get(userId.toString());

        BookingWorker bookingWorker = userWorker.getWorker(sessionId);
        if (date != null) {
            bookingWorker.removeOneDateFromList(date);
        }

        if (time != null) {
            bookingWorker.removeOneTimeFromList(time);
        }

        return true;
    }

}
