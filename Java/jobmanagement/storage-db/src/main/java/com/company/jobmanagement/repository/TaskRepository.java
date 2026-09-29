package com.company.jobmanagement.repository;

import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.enums.TimeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Eagerly loads the single-valued associations TaskMapper needs
     * (taskType, createdBy) so mapping to TaskResponse doesn't touch a lazy
     * Hibernate proxy after the transaction that loaded it has closed
     * (open-in-view is disabled). Collections (steps, assignments) still
     * need a separate touch inside the transaction — see TaskService.getTask.
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.taskType JOIN FETCH t.createdBy WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM Task t WHERE t.createdBy.id = :userId AND t.status != 'CANCELLED'")
    List<Task> findByCreatedById(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.status != 'CANCELLED'")
    List<Task> findByStatus(@Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.taskType.id = :taskTypeId AND t.status != 'CANCELLED'")
    List<Task> findByTaskTypeId(@Param("taskTypeId") Long taskTypeId);

    @Query("SELECT t FROM Task t WHERE t.section = :section AND t.status != 'CANCELLED'")
    List<Task> findBySection(@Param("section") String section);

    @Query("SELECT t FROM Task t WHERE t.periodMonth = :periodMonth AND t.status != 'CANCELLED'")
    List<Task> findByPeriodMonth(@Param("periodMonth") LocalDate periodMonth);

    @Query("SELECT t FROM Task t " +
           "JOIN t.assignments ta " +
           "WHERE ta.assignee.id = :userId AND ta.isCurrent = true AND t.status != 'CANCELLED'")
    List<Task> findAssignedToUser(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t WHERE t.estimateMinutes IS NULL AND t.status != 'CANCELLED'")
    List<Task> findTasksWithoutEstimate();

    @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_DATE AND t.status != 'DONE' AND t.status != 'CANCELLED'")
    List<Task> findOverdueTasks();

    /**
     * Find tasks with eager loading to prevent N+1 queries.
     * Uses JOIN FETCH to load related taskType and steps.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "JOIN FETCH t.createdBy " +
           "LEFT JOIN FETCH t.subtasks st " +
           "WHERE t.createdBy.id = :userId AND t.status != 'CANCELLED' " +
           "ORDER BY t.createdAt DESC")
    Page<Task> findByCreatedByIdWithEagerLoad(
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * Find tasks without estimate with pagination.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "WHERE t.estimateMinutes IS NULL AND t.status != 'CANCELLED' " +
           "ORDER BY t.createdAt DESC")
    Page<Task> findTasksWithoutEstimateWithPagination(Pageable pageable);

    /**
     * Find overdue tasks with eager loading.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "LEFT JOIN FETCH t.assignments ta " +
           "LEFT JOIN FETCH ta.assignee " +
           "WHERE t.dueDate < CURRENT_DATE " +
           "AND t.status != 'DONE' " +
           "AND t.status != 'CANCELLED' " +
           "ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasksWithEagerLoad();

    /**
     * Find tasks by status and creator with pagination.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "JOIN FETCH t.createdBy " +
           "LEFT JOIN FETCH t.subtasks st " +
           "WHERE t.status = :status " +
           "AND t.createdBy.id = :creatorId " +
           "AND t.status != 'CANCELLED' " +
           "ORDER BY t.createdAt DESC")
    Page<Task> findByStatusAndCreator(
            @Param("status") TaskStatus status,
            @Param("creatorId") Long creatorId,
            Pageable pageable);

    /**
     * Find tasks by multiple criteria (status, type, section, date range).
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "JOIN FETCH t.createdBy " +
           "LEFT JOIN FETCH t.subtasks st " +
           "WHERE (:status IS NULL OR t.status = :status) " +
           "AND (:taskTypeId IS NULL OR t.taskType.id = :taskTypeId) " +
           "AND (:section IS NULL OR t.section = :section) " +
           "AND (:dueDateFrom IS NULL OR t.dueDate >= :dueDateFrom) " +
           "AND (:dueDateTo IS NULL OR t.dueDate <= :dueDateTo) " +
           "AND t.status != 'CANCELLED' " +
           "ORDER BY t.createdAt DESC")
    Page<Task> findByMultipleCriteria(
            @Param("status") TaskStatus status,
            @Param("taskTypeId") Long taskTypeId,
            @Param("section") String section,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo,
            Pageable pageable);

    /**
     * Backs the Kanban board (Scope.md §2.0). Only single-valued associations
     * are JOIN FETCHed (taskType, createdBy) — steps/current-assignment are
     * batch-loaded separately by the service to dodge Hibernate's
     * one-collection-per-query limit and avoid a Cartesian blow-up.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "JOIN FETCH t.createdBy " +
           "LEFT JOIN t.assignments ta ON ta.isCurrent = true " +
           "WHERE t.status != com.company.jobmanagement.model.enums.TaskStatus.CANCELLED " +
           "AND (:timeCategory IS NULL OR tt.timeCategory = :timeCategory) " +
           "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "AND (:assigneeId IS NULL OR ta.assignee.id = :assigneeId) " +
           "ORDER BY t.dueDate ASC NULLS LAST")
    List<Task> findForKanban(
            @Param("timeCategory") TimeCategory timeCategory,
            @Param("search") String search,
            @Param("assigneeId") Long assigneeId);

    /**
     * Backs the Timeline/Gantt view (Scope.md §5.5) — one assignee's tasks,
     * optionally excluding completed ones ("Xem tất cả" toggle).
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "JOIN t.assignments ta ON ta.isCurrent = true " +
           "WHERE ta.assignee.id = :assigneeId " +
           "AND t.status != com.company.jobmanagement.model.enums.TaskStatus.CANCELLED " +
           "AND (:includeCompleted = true OR t.status NOT IN (" +
           "  com.company.jobmanagement.model.enums.TaskStatus.DONE, " +
           "  com.company.jobmanagement.model.enums.TaskStatus.CLOSED_LATE)) " +
           "ORDER BY t.dueDate ASC NULLS LAST")
    List<Task> findForTimeline(
            @Param("assigneeId") Long assigneeId,
            @Param("includeCompleted") boolean includeCompleted);

    /**
     * Find tasks for KPI calculation.
     * For a given user and period, return all tasks with their details.
     * Used by KPI calculation service to compute WCR, VI, EA indices.
     * Scope.md §13: passing groupId scopes the calculation to only that
     * group's tasks, so a multi-group member's KPI is genuinely split, not
     * merged; null keeps the pre-§13 "all of the user's tasks" behavior.
     */
    @Query("SELECT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "LEFT JOIN FETCH t.assignments ta " +
           "WHERE ta.assignee.id = :userId " +
           "AND ta.isCurrent = true " +
           "AND YEAR(t.dueDate) = :year " +
           "AND MONTH(t.dueDate) = :month " +
           "AND t.status != 'CANCELLED' " +
           "AND (:groupId IS NULL OR t.group.id = :groupId)")
    List<Task> findTasksForKpiCalculation(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("groupId") Long groupId);

    /**
     * Find tasks due tomorrow with eager loading.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "LEFT JOIN FETCH t.assignments ta " +
           "LEFT JOIN FETCH ta.assignee " +
           "WHERE CAST(t.dueDate AS date) = :tomorrow " +
           "AND t.status NOT IN ('DONE', 'CANCELLED') " +
           "ORDER BY t.dueDate ASC")
    List<Task> findTasksDueTomorrow(@Param("tomorrow") LocalDate tomorrow);

    /**
     * Find tasks due within specific date range.
     */
    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.taskType tt " +
           "LEFT JOIN FETCH t.assignments ta " +
           "LEFT JOIN FETCH ta.assignee " +
           "WHERE t.dueDate >= :startDate " +
           "AND t.dueDate <= :endDate " +
           "AND t.status NOT IN ('DONE', 'CANCELLED') " +
           "ORDER BY t.dueDate ASC")
    List<Task> findTasksDueInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
