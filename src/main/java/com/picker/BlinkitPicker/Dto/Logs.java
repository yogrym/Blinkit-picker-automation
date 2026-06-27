package com.picker.BlinkitPicker.Dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class Logs {
    private List<String> logs;
}
