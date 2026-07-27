package com.picker.BlinkitPicker.Model;

import com.picker.BlinkitPicker.Enums.RoleEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_api_key", columnList = "api_key"),
        @Index(name = "idx_users_expired", columnList = "expired"),
        @Index(name = "idx_users_created_at", columnList = "created_at")
})
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_username")
    private String telegramUserName;

    @Column(unique = true, name = "phone_number")
    private String phone;

    @Column(name = "acessToken", nullable = true, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", nullable = true, columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "api_key", nullable = true)
    private String apiKey;

    @Column(name = "total_booked_slots", nullable = true)
    private Long totalBookedSlots;

    @Builder.Default
    private Boolean expired = false;

    @Builder.Default
    private Boolean blocked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_headers", nullable = true, columnDefinition = "jsonb")
    private UserHeaderModel userHeaders;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private StoreModel store;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RoleEnum role;
}
