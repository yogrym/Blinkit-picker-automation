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
public class SinglePollingResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("data")
    private DataInfo data; // Nullable (Optional)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataInfo {
        @JsonProperty("tasks")
        private List<TaskInfo> tasks; // Nullable (Optional)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskInfo {
        @JsonProperty("task_type")
        private String taskType; // Nullable (Optional)

        @JsonProperty("task_details")
        private PickListResponse taskDetails; // Nullable (Optional)
    }
}
