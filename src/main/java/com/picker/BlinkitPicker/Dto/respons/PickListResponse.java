package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickListResponse {

    @JsonProperty("activityId")
    private Long activityId; // Non-nullable

    // Nullable (Optional) fields below
    @JsonProperty("erId")
    private Long erId; 

    @JsonProperty("totalItemCount")
    private Integer totalItemCount; 

    @JsonProperty("actStatus")
    private String actStatus; 

    @JsonProperty("itemList")
    private List<PicklistItemResponse> itemList; 

    @JsonProperty("packagingBagList")
    private List<Object> packagingBagList; 
}
