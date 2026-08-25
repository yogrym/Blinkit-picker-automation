package com.picker.BlinkitPicker.Dto.respons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSendOtpResponse {
    
    private String status;
    private String action;
    private String msg;
}
