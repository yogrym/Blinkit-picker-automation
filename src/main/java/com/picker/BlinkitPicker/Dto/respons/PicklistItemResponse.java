package com.picker.BlinkitPicker.Dto.respons;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PicklistItemResponse {

    @JsonProperty("id")
    private Long id; // Non-nullable

    @JsonProperty("item_id")
    private String itemId; // Non-nullable

    // Nullable (Optional) fields below
    @JsonProperty("item_name")
    private String itemName; 

    @JsonProperty("item_image_url")
    private String itemImageUrl; 

    @JsonProperty("item_type")
    private String itemType; 

    @JsonProperty("item_uom_text")
    private String itemUomText; 

    @JsonProperty("item_mrp")
    private Double itemMrp; 

    @JsonProperty("required_quantity")
    private Integer requiredQuantity; 

    @JsonProperty("picked_quantity")
    private Integer pickedQuantity; 

    @JsonProperty("shorted_quantity")
    private Integer shortedQuantity; 

    @JsonProperty("location_name")
    private String locationName; 

    @JsonProperty("storage_type")
    private String storageType; 

    @JsonProperty("upc_list")
    private Set<String> upcList; 

    @JsonProperty("upc_scan")
    private Boolean upcScan; 

    @JsonProperty("upc_scan_override")
    private Boolean upcScanOverride; 

    @JsonProperty("scan_location")
    private Boolean scanLocation; 

    @JsonProperty("skip_location_scan")
    private Boolean skipLocationScan; 

    @JsonProperty("bag_scan_required")
    private Boolean bagScanRequired; 

    @JsonProperty("image_capture_required")
    private Boolean imageCaptureRequired; 

    @JsonProperty("gift_item")
    private Boolean giftItem; 

    @JsonProperty("masked")
    private Boolean masked; 

    @JsonProperty("remove_from_pick_list")
    private Boolean removeFromPickList; 

    @JsonProperty("show_prescription")
    private Boolean showPrescription; 

    @JsonProperty("er_line_id")
    private Long erLineId; 

    @JsonProperty("outer_case_size")
    private String outerCaseSize; 

    @JsonProperty("packaging_type")
    private String packagingType; 
}
