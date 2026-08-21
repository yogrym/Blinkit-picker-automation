package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuccessfullOtpResponse {

    private boolean login;
    private String action;

    @JsonProperty("sms_sent")
    private boolean smsSent;

    @JsonProperty("message_id")
    private String messageId;

    private boolean success;
    private String message;
}