package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full KPI breakdown popup (Scope.md §5.6.4).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Full KPI breakdown for one member, one period")
public class MemberKpiDetailResponse {

    private Long userId;
    private String fullName;
    private String role;
    private String periodMonth;

    @Schema(description = "Group this breakdown is scoped to; null for a member with no group membership (Scope.md §13)")
    private Long groupId;
    private String groupName;

    private BigDecimal kpiFinal;
    private String kpiStatus;
    private BigDecimal previousMonthKpi;
    private BigDecimal changeVsPreviousMonth;

    private BigDecimal autoScore;
    private BigDecimal leadScore;
    private BigDecimal managerScore;
    private Boolean leadHasScored;
    private Boolean managerHasFinalized;

    private TypeBreakdown fastTask;
    private TypeBreakdown multiStep;

    private List<TaskLine> tasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Per-type completion breakdown row")
    public static class TypeBreakdown {
        private Integer done;
        private Integer total;
        private BigDecimal completionPercent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "One task row in the period's task list")
    public static class TaskLine {
        private Long taskId;
        private String title;
        private String timeCategory;
        private String status;
        private LocalDate dueDate;
        private java.time.ZonedDateTime completedAt;
    }
}
