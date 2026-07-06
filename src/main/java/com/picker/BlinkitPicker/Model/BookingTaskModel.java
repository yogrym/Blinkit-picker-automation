package com.picker.BlinkitPicker.Model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "booking_tasks", indexes = {
        @Index(name = "idx_booking_tasks_user_active", columnList = "user_id, active"),
        @Index(name = "idx_booking_tasks_active", columnList = "active")
})
public class BookingTaskModel {

    @Id
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dates", nullable = false, columnDefinition = "jsonb")
    private List<String> dates;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "times", nullable = false, columnDefinition = "jsonb")
    private List<String> times;

    @Builder.Default
    @Column(name = "paused", nullable = false)
    private Boolean paused = false;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "first_date")
    private String firstDate;

    @Column(name = "last_date")
    private String lastDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
