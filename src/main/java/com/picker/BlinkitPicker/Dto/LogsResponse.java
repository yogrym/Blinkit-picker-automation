package com.picker.BlinkitPicker.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LogsResponse {
    private List<Logs> logs;
    private boolean isReset;
}
