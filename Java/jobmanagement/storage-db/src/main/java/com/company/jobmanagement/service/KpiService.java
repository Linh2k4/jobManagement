package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class KpiService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public KpiResponse calculateKpi(Long userId, LocalDate periodMonth) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        YearMonth yearMonth = YearMonth.from(periodMonth);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        List<Task> allTasks = taskRepository.findAll().stream()
                .filter(t -> t.getCreatedBy().getId().equals(userId) &&
                            t.getCreatedAt().toLocalDate().isAfter(monthStart.minusDays(1)) &&
                            t.getCreatedAt().toLocalDate().isBefore(monthEnd.plusDays(1)))
                .toList();

        List<Task> tasksWithEstimate = allTasks.stream()
                .filter(Task::hasEstimate)
                .toList();

        long completedTasks = tasksWithEstimate.stream()
                .filter(Task::isDone)
                .count();

        double completionRate = tasksWithEstimate.isEmpty() ? 0.0 :
                (completedTasks * 100.0) / (long)tasksWithEstimate.size();

        boolean hasTasksWithoutEstimate = allTasks.size() > tasksWithEstimate.size();

        return KpiResponse.builder()
                .userId(userId)
                .userName(user.getFullName())
                .periodMonth(periodMonth)
                .totalTasks((long)tasksWithEstimate.size())
                .completedTasks(completedTasks)
                .completionRate(completionRate)
                .tasksWithoutEstimateCount(allTasks.size() - tasksWithEstimate.size())
                .hasWarning(hasTasksWithoutEstimate)
                .build();
    }

    public TeamKpiResponse calculateTeamKpi(Long leadOrManagerId, LocalDate periodMonth) {
        User lead = userRepository.findById(leadOrManagerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + leadOrManagerId));

        if (!lead.isLead() && !lead.isManager()) {
            throw new RuntimeException("User must be Lead or Manager");
        }

        YearMonth yearMonth = YearMonth.from(periodMonth);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        List<User> teamMembers = lead.isManager() ?
                userRepository.findAll().stream()
                        .filter(u -> lead.getId().equals(u.getManager().getId()))
                        .toList() :
                userRepository.findAll().stream()
                        .filter(u -> lead.getId().equals(u.getLead().getId()))
                        .toList();

        List<KpiResponse> memberKpis = teamMembers.stream()
                .map(member -> calculateKpi(member.getId(), periodMonth))
                .toList();

        double avgCompletionRate = memberKpis.stream()
                .mapToDouble(KpiResponse::getCompletionRate)
                .average()
                .orElse(0.0);

        long totalTasks = memberKpis.stream()
                .mapToLong(KpiResponse::getTotalTasks)
                .sum();

        return TeamKpiResponse.builder()
                .leaderId(leadOrManagerId)
                .leaderName(lead.getFullName())
                .leaderRole(lead.getRole().toString())
                .periodMonth(periodMonth)
                .teamSize(teamMembers.size())
                .avgCompletionRate(avgCompletionRate)
                .totalTeamTasks(totalTasks)
                .memberKpis(memberKpis)
                .build();
    }
}
