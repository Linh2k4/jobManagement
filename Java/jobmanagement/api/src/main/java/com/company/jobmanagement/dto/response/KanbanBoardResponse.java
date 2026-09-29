package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Kanban board: tasks bucketed into the three status columns (Scope.md §2.0.2)")
public class KanbanBoardResponse {

    private List<KanbanTaskResponse> todo;
    private List<KanbanTaskResponse> doing;
    private List<KanbanTaskResponse> done;
}
