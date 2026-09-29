package com.company.jobmanagement.dto.request;

import com.company.jobmanagement.model.enums.TaskStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateTaskStatusRequest {
    @NotEmpty(message = "Task IDs cannot be empty")
    private List<Long> taskIds;

    @NotNull(message = "New status is required")
    private TaskStatus newStatus;

    private String reason;
}
