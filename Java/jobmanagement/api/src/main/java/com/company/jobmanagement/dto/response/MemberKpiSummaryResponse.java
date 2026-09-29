package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One row of the Members & KPI view (Scope.md §5.6.3).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Member row with current-month KPI summary")
public class MemberKpiSummaryResponse {

    private Long userId;
    private String fullName;
    private String role;

    @Schema(description = "Group this row's KPI is scoped to; null for a member with no group membership (Scope.md §13)")
    private Long groupId;
    private String groupName;

    @Schema(description = "kpiFinal if the evaluation cycle set it, else the real-time autoScore")
    private BigDecimal kpi;

    @Schema(allowableValues = {"DAT", "CANH_BAO", "NGUY_HIEM"})
    private String kpiStatus;

    private Integer fastDone;
    private Integer fastTotal;
    private Integer multiStepDone;
    private Integer multiStepTotal;
    private Integer totalDone;
    private Integer overdueCount;
}
