package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Request & approval flow for pushing a task's deadline back (Scope.md §12).
 * A Member's request needs their Lead (or Manager); a Lead's request needs
 * a Manager; a Manager's request self-approves (§12.7).
 */
@Entity
@Table(name = "deadline_extension_requests", indexes = {
    @Index(name = "idx_dext_task_id", columnList = "task_id"),
    @Index(name = "idx_dext_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadlineExtensionRequest {

    public enum Status { PENDING, APPROVED, REJECTED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(nullable = false)
    private LocalDate currentDeadline;

    @Column(nullable = false)
    private LocalDate requestedDeadline;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column
    private ZonedDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    /** Which extension attempt this is for the task (1-based), for the max-count rule. */
    @Column(nullable = false)
    private Integer extensionNumber;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false)
    private ZonedDateTime expiresAt;

    public boolean isPending() {
        return status == Status.PENDING;
    }
}
