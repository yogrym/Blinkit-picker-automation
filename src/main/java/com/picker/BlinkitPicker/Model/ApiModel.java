package com.picker.BlinkitPicker.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Entity
@Table(name="server_api_configs")
public class ApiModel {

    @Id
    @Column(name = "api_name", nullable = false, updatable = false)
    private String apiName;

    @Column(name = "api_url", nullable = false)
    private String apiUrl;

}
