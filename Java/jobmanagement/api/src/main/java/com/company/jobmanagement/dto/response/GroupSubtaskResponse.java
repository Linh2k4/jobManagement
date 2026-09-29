package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Group header only — its subtasks live in {@code TaskResponse.subtasks},
 * each carrying this group's id, so the client builds the tree itself.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "GroupSubtask", description = "Optional group header for a task's subtasks")
public class GroupSubtaskResponse {

    private Long id;
    private String name;
    private Integer groupOrder;
}
