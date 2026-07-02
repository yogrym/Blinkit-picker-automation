package com.picker.BlinkitPicker.Dto;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.picker.BlinkitPicker.Services.BookingWorker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class WorkerList {

    private final ConcurrentHashMap<String, BookingWorker> workerList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BookingData> bookingDataMap = new ConcurrentHashMap<>();

    public void addWorker(String sessionId, BookingWorker workerObj) {
        workerList.put(sessionId, workerObj);
    }

    public void removeWorker(String sessionId) {
        workerList.remove(sessionId);
        bookingDataMap.remove(sessionId);
    }

    public BookingWorker getWorker(String sessionId) {
        return workerList.get(sessionId);
    }

    public ConcurrentHashMap<String, BookingWorker> getAllWorkers() {
        return workerList;
    }

    public void addBookingData(Boolean isPaused, String sessionId, String firstDate, String lastDate) {
        bookingDataMap.put(sessionId, new BookingData(isPaused, sessionId, firstDate, lastDate));
    }

    public List<BookingData> getAllBookingData() {
        return new ArrayList<>(bookingDataMap.values());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookingData {

        private Boolean isPaused;
        private String sessionId;
        private String firstDate;
        private String lastDate;
    }
}
