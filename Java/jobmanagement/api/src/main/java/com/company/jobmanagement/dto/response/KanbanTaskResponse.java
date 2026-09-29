package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.enums.TimeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Lightweight task projection for the Kanban board (Scope.md §2.0.3) —
 * only what a card needs, not the full TaskResponse graph.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Task card for the Kanban board")
public class KanbanTaskResponse {

    private Long id;
    private String title;
    private TimeCategory timeCategory;
    private String priority;
    private String status;
    private LocalDate dueDate;
    private Boolean isOverdue;
    private Integer stepsDone;
    private Integer stepsTotal;
    private Integer commentCount;
    private Integer attachmentCount;
    private UserInfoResponse assignee;
}
