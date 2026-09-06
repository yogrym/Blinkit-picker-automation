package com.picker.BlinkitPicker.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.picker.BlinkitPicker.Model.BookingTaskModel;

@Repository
public interface BookingTaskRepo extends JpaRepository<BookingTaskModel, String> {

    List<BookingTaskModel> findByActiveTrue();

    Optional<BookingTaskModel> findByUserIdAndSessionIdAndActiveTrue(Long userId, String sessionId);

    /**
     * Finds all active booking tasks belonging to a specific user.
     * Used during OTP verification and scheduler to propagate updated tokens
     * across every session of a given user.
     */
    List<BookingTaskModel> findByUserIdAndActiveTrue(Long userId);

    List<BookingTaskModel> findByUserId(Long userId);
}
