package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpAuthRespons {


    @JsonProperty("login")
    private boolean login;

    @JsonProperty("action")
    private String action;

    @JsonProperty("sms_sent")
    private boolean smsSent;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    
}
