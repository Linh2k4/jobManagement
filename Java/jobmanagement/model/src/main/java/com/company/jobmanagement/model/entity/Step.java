package com.company.jobmanagement.model.entity;

import com.company.jobmanagement.model.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/** Leaf of a MULTI_STEP task's Subtask. Has its own assignee/deadline. */
@Entity
@Table(name = "steps", indexes = {
    @Index(name = "idx_steps_subtask_id", columnList = "subtask_id"),
    @Index(name = "idx_steps_assignee_id", columnList = "assignee_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Step {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtask_id", nullable = false)
    private Subtask subtask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_step_id")
    private TaskTypeStep taskTypeStep;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column
    private Integer estimateMinutes;

    @Column
    private ZonedDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;

    @Column
    private ZonedDateTime completedAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    public boolean isPending() {
        return status == StepStatus.PENDING;
    }

    public boolean isInProgress() {
        return status == StepStatus.IN_PROGRESS;
    }

    public boolean isDone() {
        return status == StepStatus.DONE;
    }
}
