package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * One task (FAST) or step (MULTI_STEP) affected by a member's approved
 * leave, with a suggested reassignment for the Lead to confirm (Scope.md
 * §14.3/§14.4).
 */
@Entity
@Table(name = "handover_suggestions", indexes = {
    @Index(name = "idx_handover_suggestions_leave_request_id", columnList = "leave_request_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandoverSuggestion {

    public enum EntityType { FAST_TASK, STEP }
    public enum Action { REASSIGN, EXTEND_DEADLINE, SKIP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequest leaveRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /** Set only when entityType=STEP — the specific step being reassigned (task itself keeps its assignee). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id")
    private Step step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType entityType;

    @Column
    private ZonedDateTime currentDeadline;

    /** Lowest-workload teammate at generation time; null if the Lead's team has no other member. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_assignee_id")
    private User suggestedAssignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Action action = Action.REASSIGN;

    @Column(nullable = false)
    @Builder.Default
    private Boolean confirmed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_assignee_id")
    private User confirmedAssignee;

    @Column
    private ZonedDateTime confirmedAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
}
