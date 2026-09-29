package com.company.jobmanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;

/**
 * Evaluation period lifecycle (month/quarter/year).
 * Controls when employees can submit self-evaluations and when leads/managers can review.
 */
@Entity
@Table(name = "evaluation_periods", indexes = {
    @Index(name = "idx_eval_period_month", columnList = "period_month"),
    @Index(name = "idx_eval_period_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private YearMonth periodMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    @Column(nullable = false)
    private LocalDate openDate;  // When period opens (members can start self-eval)

    @Column(nullable = false)
    private LocalDate memberDeadline;  // Deadline for member self-evaluation

    @Column(nullable = false)
    private LocalDate leadDeadline;  // Deadline for lead evaluation

    @Column(nullable = false)
    private LocalDate managerDeadline;  // Deadline for manager finalization

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    private Long version;

    public boolean isOpen() {
        return status == EvaluationStatus.OPEN;
    }

    public boolean isClosed() {
        return status == EvaluationStatus.FINALIZED;
    }

    @Getter
    public enum EvaluationStatus {
        DRAFT("Dự thảo"),
        OPEN("Mở"),
        SUBMITTED("Đã gửi"),
        REVIEWED("Đã kiểm duyệt"),
        FINALIZED("Hoàn thành");

        private final String displayName;

        EvaluationStatus(String displayName) {
            this.displayName = displayName;
        }
    }
}
