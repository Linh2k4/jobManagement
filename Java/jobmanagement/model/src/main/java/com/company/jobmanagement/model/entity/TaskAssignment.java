package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "task_assignments", indexes = {
    @Index(name = "idx_task_assignments_task_id", columnList = "task_id"),
    @Index(name = "idx_task_assignments_assignee_id", columnList = "assignee_id"),
    @Index(name = "idx_task_assignments_assigned_by", columnList = "assigned_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCurrent = true;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime assignedAt = ZonedDateTime.now();
}
