package com.picker.BlinkitPicker.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Entity
public class ApiModel {

    @Column(name = "api_name", nullable = false)
    private String apiName;

    @Column(name = "api_url", nullable = false)
    private String apiUrl;

}
