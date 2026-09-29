package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_type_subtasks", indexes = {
    @Index(name = "idx_task_type_subtasks_task_type_id", columnList = "task_type_id"),
    @Index(name = "idx_task_type_subtasks_group_id", columnList = "task_type_group_subtask_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTypeSubtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_group_subtask_id")
    private TaskTypeGroupSubtask taskTypeGroupSubtask;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private Integer subtaskOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isTarget = false;

    @Column
    private Integer estimateTarget;

    @OneToMany(mappedBy = "taskTypeSubtask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskTypeStep> steps = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
}
