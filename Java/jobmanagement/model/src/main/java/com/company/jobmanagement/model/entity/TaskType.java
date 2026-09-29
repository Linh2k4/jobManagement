package com.company.jobmanagement.model.entity;

import com.company.jobmanagement.model.enums.OrganizeBy;
import com.company.jobmanagement.model.enums.TimeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_types", indexes = {
    @Index(name = "idx_task_types_code", columnList = "code"),
    @Index(name = "idx_task_types_time_category", columnList = "time_category"),
    @Index(name = "idx_task_types_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeCategory timeCategory;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasEstimate = true;

    @Column
    private Integer defaultEstimateMinutes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isMultiStep = false;

    @Enumerated(EnumType.STRING)
    @Column
    private OrganizeBy organizeBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "taskType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskTypeGroupSubtask> groupSubtasks = new ArrayList<>();

    @OneToMany(mappedBy = "taskType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskTypeSubtask> subtasks = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    public boolean isFastTask() {
        return timeCategory == TimeCategory.FAST;
    }

    public boolean isOftenTask() {
        return timeCategory == TimeCategory.OFTEN;
    }

    public boolean isMultiStepTask() {
        return timeCategory == TimeCategory.MULTI_STEP;
    }
}
