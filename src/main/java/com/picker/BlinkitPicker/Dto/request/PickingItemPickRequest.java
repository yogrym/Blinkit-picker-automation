package com.picker.BlinkitPicker.Dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PickingItemPickRequest {

    @JsonProperty("act_id")
    private Long actId; // Non-nullable (Mandatory)

    @JsonProperty("ia_id")
    private Long iaId; // Non-nullable (Mandatory)

    @JsonProperty("upc_id")
    private String upcId; // Non-nullable (Mandatory)

    @JsonProperty("scanned_location")
    private String scannedLocation; // Non-nullable (Mandatory)

    @JsonProperty("suggested_location")
    private String suggestedLocation; // Non-nullable (Mandatory)

    @JsonProperty("bag_qr_code")
    private String bagQrCode; // Nullable (Optional)

    @JsonProperty("picked_quantity")
    private Integer pickedQuantity; // Non-nullable (Mandatory)

    @JsonProperty("suggested_quantity")
    private Integer suggestedQuantity; // Non-nullable (Mandatory)

    @JsonProperty("scanned")
    private Boolean scanned; // Non-nullable (Mandatory)

    // Optional fields below can be null (not necessary)
    @JsonProperty("deviceType")
    private String deviceType; 

    @JsonProperty("treated_as")
    private String treatedAs; 

    @JsonProperty("variant_id")
    private String variantId; 

    @JsonProperty("image_file_name_vs_path_map")
    private Map<String, String> imageFileNameVsPathMap; 

    @JsonProperty("picked_batch_details")
    private List<Object> pickedBatchDetails; 
}
