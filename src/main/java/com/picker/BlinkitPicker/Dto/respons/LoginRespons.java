package com.picker.BlinkitPicker.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginRespons {
    
    private String token;
    private String refreshtoken;
    private String message;
}
