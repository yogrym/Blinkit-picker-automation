package com.picker.BlinkitPicker.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="server_api_configs")
public class ApiModel {

    @Id
    @Column(name = "api_name", nullable = false, updatable = false)
    private String apiName;

    @Column(name = "api_url", nullable = false)
    private String apiUrl;

}
