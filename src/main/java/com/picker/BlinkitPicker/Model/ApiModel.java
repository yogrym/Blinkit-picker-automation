package com.picker.BlinkitPicker.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Entity
@Table(name = "api_config")
public class ApiModel {

    private String key;
    private String value;
}
