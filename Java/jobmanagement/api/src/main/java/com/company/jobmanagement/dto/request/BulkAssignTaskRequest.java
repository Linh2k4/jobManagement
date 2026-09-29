package com.company.jobmanagement.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignTaskRequest {
    @NotEmpty(message = "Task IDs cannot be empty")
    private List<Long> taskIds;

    @NotNull(message = "Assignee ID is required")
    private Long assigneeId;

    private String notes;
}
