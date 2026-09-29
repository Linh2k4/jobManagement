package com.company.jobmanagement.model.entity;

import com.company.jobmanagement.model.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Leaf for a FAST task (ticked done manually — estimateMinutes/deadline/note
 * apply here); parent-of-steps for a MULTI_STEP task (isTarget/estimateTarget/
 * target apply here, status/completion derives from its steps instead).
 */
@Entity
@Table(name = "subtasks", indexes = {
    @Index(name = "idx_subtasks_task_id", columnList = "task_id"),
    @Index(name = "idx_subtasks_group_id", columnList = "group_subtask_id"),
    @Index(name = "idx_subtasks_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_subtask_id")
    private GroupSubtask groupSubtask;

    @Column(nullable = false)
    private Integer subtaskOrder;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;

    // FAST-only
    @Column
    private Integer estimateMinutes;

    @Column
    private ZonedDateTime deadline;

    @Column(columnDefinition = "TEXT")
    private String note;

    // MULTI_STEP-only
    @Column(nullable = false)
    @Builder.Default
    private Boolean isTarget = false;

    @Column
    private Integer estimateTarget;

    @Column
    private Integer target;

    @OneToMany(mappedBy = "subtask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Step> steps = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    public boolean isDone() {
        return status == StepStatus.DONE;
    }
}
