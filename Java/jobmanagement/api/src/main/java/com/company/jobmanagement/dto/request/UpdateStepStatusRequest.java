package com.company.jobmanagement.dto.request;

import com.company.jobmanagement.model.enums.StepStatus;
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
@Schema(name = "UpdateStepStatusRequest", description = "Request to update step status")
public class UpdateStepStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(
        description = "New step status",
        example = "IN_PROGRESS",
        allowableValues = {"PENDING", "IN_PROGRESS", "DONE"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private StepStatus status;
}
