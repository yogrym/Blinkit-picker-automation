package com.picker.BlinkitPicker.Dto;

import java.util.concurrent.ConcurrentHashMap;

import com.picker.BlinkitPicker.Services.BookingWorker;

import lombok.Builder;
import lombok.Data;

@Data
public class WorkerList {

    private final ConcurrentHashMap<String, BookingWorker> workerList = new ConcurrentHashMap<>();

    public void addWorker(String sessionId, BookingWorker workerObj) {
        workerList.put(sessionId, workerObj);
    }

    public void removeWorker(String sessionId) {
        workerList.remove(sessionId);
    }

    public BookingWorker getWorker(String sessionId) {
        return workerList.get(sessionId);
    }

    public ConcurrentHashMap<String, BookingWorker> getAllWorkers() {
        return workerList;
    }

}
