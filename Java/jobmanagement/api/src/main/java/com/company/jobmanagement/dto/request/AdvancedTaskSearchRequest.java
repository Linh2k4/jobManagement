package com.company.jobmanagement.dto.request;

import com.company.jobmanagement.model.enums.Priority;
import com.company.jobmanagement.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedTaskSearchRequest {
    private String keyword;  // Search in title/description
    private TaskStatus status;
    private Priority priority;
    private Long taskTypeId;
    private Long createdById;
    private Long assignedToUserId;
    private LocalDate dueDateFrom;
    private LocalDate dueDateTo;
    private String section;
    private Boolean hasEstimate;
    private Boolean isOverdue;

    // Pagination handled by controller
}
