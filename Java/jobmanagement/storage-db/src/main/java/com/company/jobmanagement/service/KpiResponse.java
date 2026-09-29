package com.company.jobmanagement.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiResponse {
    private Long userId;
    private String userName;
    private LocalDate periodMonth;
    private Long totalTasks;
    private Long completedTasks;
    private Double completionRate;
    private Integer tasksWithoutEstimateCount;
    private Boolean hasWarning;
}
