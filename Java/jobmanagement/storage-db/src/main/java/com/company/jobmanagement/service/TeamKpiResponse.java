package com.company.jobmanagement.service;

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
    private Long leaderId;
    private String leaderName;
    private String leaderRole;
    private LocalDate periodMonth;
    private Integer teamSize;
    private Double avgCompletionRate;
    private Long totalTeamTasks;
    private List<KpiResponse> memberKpis;
}
