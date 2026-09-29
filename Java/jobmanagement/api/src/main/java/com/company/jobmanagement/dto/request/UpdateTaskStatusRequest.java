package com.company.jobmanagement.dto.request;

import com.company.jobmanagement.model.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UpdateTaskStatusRequest", description = "Request to update task status")
public class UpdateTaskStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(
        description = "New task status",
        example = "IN_PROGRESS",
        allowableValues = {"PENDING", "IN_PROGRESS", "DONE", "CANCELLED"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private TaskStatus status;
}
