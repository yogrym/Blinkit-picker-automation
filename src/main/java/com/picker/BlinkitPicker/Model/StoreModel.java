package com.picker.BlinkitPicker.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "stores")
public class StoreModel {

    @Id
    @Column(name = "store_id", nullable = false, unique = true)
    private String storeId;

    @Column(name = "store_name")
    private String storeName;

    private Boolean available;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private Integer maxUsers = 3;

    @Column(name = "total_user_count")
    private Integer totalUserCount;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
