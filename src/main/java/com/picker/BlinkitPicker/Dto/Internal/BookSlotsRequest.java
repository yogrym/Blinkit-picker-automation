package com.picker.BlinkitPicker.Dto.Internal;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookSlotsRequest {

    @JsonProperty("slot_ids")
    private List<String> slotIds;
}
