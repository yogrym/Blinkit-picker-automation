package com.picker.BlinkitPicker.Dto.DateAndTimeList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class TimesList {
    private List<String> times;
}
