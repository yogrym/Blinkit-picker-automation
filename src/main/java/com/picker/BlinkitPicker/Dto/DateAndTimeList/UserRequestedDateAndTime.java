package com.picker.BlinkitPicker.Dto.DateAndTimeList;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder 
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class UserRequestedDateAndTime {
   private LinkedHashMap<String,LinkedHashSet<TimesList>> DateAndTime ;
}
