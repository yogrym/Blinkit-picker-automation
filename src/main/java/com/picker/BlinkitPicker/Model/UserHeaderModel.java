package com.picker.BlinkitPicker.Model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserHeaderModel {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;



    @JsonProperty("employee_id")
    private String employeeId;


    @JsonProperty("phone")
    private String phone;

    @JsonProperty("name")
    private String employeeName;

    @JsonProperty("user-id")
    private String userId;

    @JsonProperty("active_site_id")
    private String siteId;

    @JsonProperty("site_name")
    private String siteName;

    private String role;


    @JsonProperty("x-lat")
    private double xLat;

    @JsonProperty("x-long")
    private double xLong;
}
