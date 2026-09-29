package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.enums.TimeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * One Gantt bar (Scope.md §5.5.3). The schema has no separate task
 * start-date field, so the bar approximates start = createdAt's date and
 * end = dueDate (or start + 1 day if there's no dueDate).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "One task bar on the Timeline/Gantt view")
public class TimelineTaskResponse {

    private Long id;
    private String title;
    private TimeCategory timeCategory;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isOverdue;
    private Boolean isDone;
}
