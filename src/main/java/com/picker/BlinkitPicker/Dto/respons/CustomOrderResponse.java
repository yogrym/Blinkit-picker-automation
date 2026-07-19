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
public class CustomOrderResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("activity_id")
    private Long activityId; // Nullable (Optional)

    @JsonProperty("items")
    private List<ItemInfo> items; // Nullable (Optional)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemInfo {
        @JsonProperty("product_name")
        private String productName; // Nullable (Optional)

        @JsonProperty("quantity")
        private Integer quantity; // Nullable (Optional)

        @JsonProperty("product_image")
        private String productImage; // Nullable (Optional)
    }
}
