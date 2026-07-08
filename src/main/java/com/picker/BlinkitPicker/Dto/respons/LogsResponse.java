package com.picker.BlinkitPicker.Dto.respons;

import lombok.Builder;
import lombok.Data;
import java.util.List;

import com.picker.BlinkitPicker.Dto.Logs;

@Data
@Builder
public class LogsResponse {
    private List<Logs> logs;
    private boolean isReset;
}
