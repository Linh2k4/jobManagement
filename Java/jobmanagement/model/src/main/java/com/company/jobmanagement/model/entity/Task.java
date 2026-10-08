package com.company.jobmanagement.model.entity;

import com.company.jobmanagement.model.enums.Difficulty;
import com.company.jobmanagement.model.enums.Priority;
import com.company.jobmanagement.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "tasks", indexes = {
    @Index(name = "idx_tasks_task_type_id", columnList = "task_type_id"),
    @Index(name = "idx_tasks_status", columnList = "status"),
    @Index(name = "idx_tasks_created_by", columnList = "created_by"),
    @Index(name = "idx_tasks_due_date", columnList = "due_date"),
    @Index(name = "idx_tasks_priority", columnList = "priority"),
    @Index(name = "idx_tasks_difficulty", columnList = "difficulty"),
    @Index(name = "idx_tasks_section", columnList = "section"),
    @Index(name = "idx_tasks_period_month", columnList = "period_month"),
    @Index(name = "idx_tasks_category_code", columnList = "category_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_type_id", nullable = false)
    private TaskType taskType;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column
    private Difficulty difficulty;

    @Column
    private Integer estimateMinutes;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualHours;

    @Column(precision = 10, scale = 2)
    private BigDecimal taskWeight;

    @Column(length = 100)
    private String categoryCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "category_data")
    @Builder.Default
    private Map<String, Object> categoryData = new HashMap<>();

    // "_log" columns are append-only event lists (DB default '[]', a JSON
    // array) — Map<String,Object> here would crash Hibernate on every load
    // (MismatchedInputException: array token where an object was expected).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "extend_log")
    @Builder.Default
    private List<Object> extendLog = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "difficulty_log")
    @Builder.Default
    private List<Object> difficultyLog = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> schedule = new HashMap<>();

    @Column
    private LocalDate dueDate;

    @Column(length = 255)
    private String section;

    @Column
    private LocalDate periodMonth;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Scope.md §13: which Group's KPI this task counts toward — set from the
    // assignee's primary Group on assignment, or explicitly when a Lead
    // assigns within a specific one of the member's several groups.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column
    private ZonedDateTime completedAt;

    // Scope.md §2.2: task has an overall target (e.g. 75/100) tracked separately
    // from its Subtasks' own isTarget/estimateTarget/target, if any.
    @Column(nullable = false)
    @Builder.Default
    private Boolean isTarget = false;

    @Column
    private Integer estimateTarget;

    @Column
    private Integer target;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GroupSubtask> groupSubtasks = new ArrayList<>();

    // Includes both grouped and ungrouped subtasks (groupSubtask == null for
    // the latter) — callers that need just the tree headers use groupSubtasks.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Subtask> subtasks = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskAssignment> assignments = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Version
    private Long version;

    public boolean hasEstimate() {
        return estimateMinutes != null && estimateMinutes > 0;
    }

    public boolean isPending() {
        return status == TaskStatus.PENDING;
    }

    public boolean isInProgress() {
        return status == TaskStatus.IN_PROGRESS;
    }

    public boolean isDone() {
        return status == TaskStatus.DONE;
    }

    public boolean isCancelled() {
        return status == TaskStatus.CANCELLED;
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public int getEstimateHours() {
        return estimateMinutes != null ? estimateMinutes / 60 : 0;
    }

    public int getDifficultyLevel() {
        return difficulty != null ? difficulty.getLevel() : 3;  // default MEDIUM
    }
}
