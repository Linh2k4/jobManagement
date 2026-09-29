package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamKpiResponse {

    @Schema(description = "Leader/Manager ID", example = "1")
    private Long leaderId;

    @Schema(description = "Leader/Manager name")
    private String leaderName;

    @Schema(description = "Leader role", example = "LEAD")
    private String leaderRole;

    @Schema(description = "Period month (YYYY-MM-01)")
    private LocalDate periodMonth;

    @Schema(description = "Team size", example = "5")
    private Integer teamSize;

    @Schema(description = "Average completion rate across team", example = "75.5")
    private Double avgCompletionRate;

    @Schema(description = "Total tasks across team", example = "50")
    private Long totalTeamTasks;

    @Schema(description = "Individual KPIs for each team member")
    private List<KpiResponse> memberKpis;
}
