package com.picker.BlinkitPicker.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.picker.BlinkitPicker.Dto.BookingRequest;
import com.picker.BlinkitPicker.Dto.WorkerList;
import com.picker.BlinkitPicker.Model.UserModel;
import com.picker.BlinkitPicker.Repository.UserRepo;
import com.picker.BlinkitPicker.Util.SessionIdGenerator;

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

    public String startBooking(String token, BookingRequest request) {

        Long userId = jwtServices.extractUserId(token);

        UserModel user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        List<String> dates = request.getDates();

        String sessionId = SessionIdGenerator.generateSessionId();

        BookingWorker worker = new BookingWorker(userId.toString(), dates, user, webClientServices);

        if (workerMap.containsKey(userId.toString())) {

            workerMap.get(userId.toString()).addWorker(sessionId, worker);
        } else {

            WorkerList userWorkers = new WorkerList();
            userWorkers.addWorker(sessionId, worker);

            workerMap.put(userId.toString(), userWorkers);
        }

        executor.submit(worker);

        return sessionId;
    }

}
