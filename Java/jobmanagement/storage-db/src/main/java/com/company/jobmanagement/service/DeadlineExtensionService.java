package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.model.entity.DeadlineExtensionRequest;
import com.company.jobmanagement.model.entity.Notification;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskAssignment;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.repository.DeadlineExtensionRequestRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Deadline extension request/approval workflow (Scope.md §12). The
 * thresholds below (§12.4/12.5) belong in System Config (§7) once that
 * exists — hardcoded here for now, same as this codebase's other
 * not-yet-configurable business constants.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeadlineExtensionService {

    private static final int MAX_EXTENSIONS = 3;
    private static final int MAX_EXTEND_DAYS = 30;
    private static final int REVIEW_HOURS = 48;
    private static final int MIN_REASON_LENGTH = 10;

    private final DeadlineExtensionRequestRepository requestRepository;
    private final TaskRepository taskRepository;
    private final CurrentUser currentUser;
    private final NotificationService notificationService;
    private final KpiCacheService kpiCacheService;

    public DeadlineExtensionRequest requestExtension(Long taskId, LocalDate requestedDeadline, String reason) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        User requester = currentUser.getCurrentUser();

        if (task.getTaskType().getTimeCategory() == TimeCategory.FAST) {
            throw new ForbiddenOperationException("FAST Task không hỗ trợ gia hạn deadline");
        }
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new ForbiddenOperationException("Không thể gia hạn task đã hoàn thành hoặc đã huỷ");
        }
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
            throw new IllegalArgumentException("Lý do gia hạn phải có ít nhất " + MIN_REASON_LENGTH + " ký tự");
        }

        LocalDate currentDeadline = task.getDueDate() != null ? task.getDueDate() : LocalDate.now();
        if (!requestedDeadline.isAfter(currentDeadline)) {
            throw new IllegalArgumentException("Deadline mới phải sau deadline hiện tại");
        }
        if (requestedDeadline.isAfter(currentDeadline.plusDays(MAX_EXTEND_DAYS))) {
            throw new IllegalArgumentException("Không thể gia hạn quá " + MAX_EXTEND_DAYS + " ngày mỗi lần");
        }

        requestRepository.findPendingByTaskId(taskId).ifPresent(r -> {
            throw new ForbiddenOperationException("Task đã có yêu cầu gia hạn đang chờ duyệt");
        });

        int nextNumber = requestRepository.findMaxExtensionNumber(taskId) + 1;
        if (nextNumber > MAX_EXTENSIONS) {
            throw new ForbiddenOperationException("Task đã đạt số lần gia hạn tối đa (" + MAX_EXTENSIONS + ")");
        }

        DeadlineExtensionRequest request = DeadlineExtensionRequest.builder()
                .task(task)
                .requestedBy(requester)
                .currentDeadline(currentDeadline)
                .requestedDeadline(requestedDeadline)
                .reason(reason)
                .status(DeadlineExtensionRequest.Status.PENDING)
                .extensionNumber(nextNumber)
                .expiresAt(ZonedDateTime.now().plusHours(REVIEW_HOURS))
                .build();
        request = requestRepository.save(request);

        if (requester.isManager()) {
            // §12.7 — Manager requests self-approve immediately.
            return approveInternal(request, requester, "Tự động duyệt (Manager)");
        }

        notifyReviewers(task, requester);
        return request;
    }

    public DeadlineExtensionRequest approve(Long requestId, String note) {
        DeadlineExtensionRequest request = getPendingOrThrow(requestId);
        User reviewer = currentUser.getCurrentUser();
        validateReviewer(request, reviewer);
        return approveInternal(request, reviewer, note);
    }

    public DeadlineExtensionRequest reject(Long requestId, String note) {
        if (note == null || note.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do từ chối là bắt buộc");
        }
        DeadlineExtensionRequest request = getPendingOrThrow(requestId);
        User reviewer = currentUser.getCurrentUser();
        validateReviewer(request, reviewer);

        request.setStatus(DeadlineExtensionRequest.Status.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(ZonedDateTime.now());
        request.setReviewNote(note);
        DeadlineExtensionRequest saved = requestRepository.save(request);

        notificationService.notify(
                request.getRequestedBy(),
                NotificationType.SYSTEM,
                "Yêu cầu gia hạn bị từ chối",
                "Yêu cầu gia hạn deadline cho task \"" + request.getTask().getTitle() + "\" đã bị từ chối: " + note,
                request.getTask().getId()
        );
        return saved;
    }

    private DeadlineExtensionRequest approveInternal(DeadlineExtensionRequest request, User reviewer, String note) {
        request.setStatus(DeadlineExtensionRequest.Status.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(ZonedDateTime.now());
        request.setReviewNote(note);
        DeadlineExtensionRequest saved = requestRepository.save(request);

        Task task = request.getTask();
        task.setDueDate(request.getRequestedDeadline());
        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            // Deadline moved forward — an in-progress task effectively stops being overdue.
        }
        task.setUpdatedAt(ZonedDateTime.now());
        taskRepository.save(task);
        invalidateKpi(task);

        User requestedBy = request.getRequestedBy();
        notificationService.notify(
                requestedBy,
                NotificationType.SYSTEM,
                "Yêu cầu gia hạn đã được duyệt",
                "Deadline cho task \"" + task.getTitle() + "\" đã được gia hạn đến " + request.getRequestedDeadline(),
                task.getId()
        );
        task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .findFirst()
                .filter(a -> !a.getAssignee().getId().equals(requestedBy.getId()))
                .ifPresent(a -> notificationService.notify(
                        a.getAssignee(),
                        NotificationType.SYSTEM,
                        "Deadline task đã thay đổi",
                        "Deadline cho task \"" + task.getTitle() + "\" đã được gia hạn đến " + request.getRequestedDeadline(),
                        task.getId()
                ));

        return saved;
    }

    private void invalidateKpi(Task task) {
        if (task.getDueDate() == null) return;
        task.getAssignments().stream()
                .filter(TaskAssignment::getIsCurrent)
                .findFirst()
                .ifPresent(a -> kpiCacheService.invalidateKpi(a.getAssignee().getId(), java.time.YearMonth.from(task.getDueDate())));
    }

    private DeadlineExtensionRequest getPendingOrThrow(Long requestId) {
        DeadlineExtensionRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline extension request not found: " + requestId));
        if (!request.isPending()) {
            throw new ForbiddenOperationException("Yêu cầu này đã được xử lý");
        }
        return request;
    }

    /**
     * Member's request → their Lead or any Manager may review.
     * Lead's request → only a Manager may review.
     */
    private void validateReviewer(DeadlineExtensionRequest request, User reviewer) {
        User requester = request.getRequestedBy();
        if (reviewer.isManager()) {
            return;
        }
        if (reviewer.isLead() && requester.isMember()
                && requester.getLead() != null && requester.getLead().getId().equals(reviewer.getId())) {
            return;
        }
        throw new ForbiddenOperationException("Bạn không có quyền duyệt yêu cầu này");
    }

    private void notifyReviewers(Task task, User requester) {
        try {
            if (requester.isMember() && requester.getLead() != null) {
                notificationService.notify(
                        requester.getLead(),
                        NotificationType.SYSTEM,
                        "Yêu cầu gia hạn deadline",
                        requester.getFullName() + " xin gia hạn deadline cho task \"" + task.getTitle() + "\"",
                        task.getId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to notify reviewer for deadline extension request on task {}", task.getId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<DeadlineExtensionRequest> getHistory(Long taskId) {
        return requestRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Transactional(readOnly = true)
    public List<DeadlineExtensionRequest> getPendingForCurrentUser() {
        User user = currentUser.getCurrentUser();
        if (user.isManager()) {
            return requestRepository.findAllPending();
        }
        if (user.isLead()) {
            return requestRepository.findPendingForLeadTeam(user.getId());
        }
        return List.of();
    }

    /**
     * §12.5 — auto-expire requests the reviewer never got to in time.
     * Expiry leaves the task's deadline untouched and doesn't count toward
     * the extension-count limit.
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void expireOverdueRequests() {
        List<DeadlineExtensionRequest> expired = requestRepository.findExpired(ZonedDateTime.now());
        for (DeadlineExtensionRequest request : expired) {
            request.setStatus(DeadlineExtensionRequest.Status.EXPIRED);
            requestRepository.save(request);
            log.info("Deadline extension request {} expired (task {})", request.getId(), request.getTask().getId());
        }
    }
}
