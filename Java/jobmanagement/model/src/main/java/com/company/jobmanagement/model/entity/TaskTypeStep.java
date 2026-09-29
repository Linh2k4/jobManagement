package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "task_type_steps", indexes = {
    @Index(name = "idx_task_type_steps_subtask_id", columnList = "task_type_subtask_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTypeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_subtask_id", nullable = false)
    private TaskTypeSubtask taskTypeSubtask;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(nullable = false, length = 255)
    private String name;

    @Column
    private Integer estimateMinutes;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
}
