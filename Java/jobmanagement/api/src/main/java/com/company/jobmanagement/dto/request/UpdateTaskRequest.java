package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UpdateTaskRequest", description = "Request to update task details")
public class UpdateTaskRequest {

    @Schema(description = "Task title/subject", example = "Updated task title")
    private String title;

    @Schema(description = "Detailed task description")
    private String description;

    @Min(value = 1, message = "Estimate must be positive if provided")
    @Schema(description = "Estimated time in minutes", example = "480")
    private Integer estimateMinutes;

    @Schema(description = "Task due date", example = "2026-07-15")
    private LocalDate dueDate;

    @Schema(description = "Team section for OFTEN tasks", example = "Finance")
    private String section;
}
