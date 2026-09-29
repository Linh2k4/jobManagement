package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.BusinessLogicException;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.model.entity.HandoverSuggestion;
import com.company.jobmanagement.model.entity.LeaveRequest;
import com.company.jobmanagement.model.entity.Step;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskAssignment;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.model.enums.NotificationType;
import com.company.jobmanagement.model.enums.StepStatus;
import com.company.jobmanagement.model.enums.TaskStatus;
import com.company.jobmanagement.model.enums.TimeCategory;
import com.company.jobmanagement.repository.HandoverSuggestionRepository;
import com.company.jobmanagement.repository.LeaveRequestRepository;
import com.company.jobmanagement.repository.StepRepository;
import com.company.jobmanagement.repository.TaskAssignmentRepository;
import com.company.jobmanagement.repository.TaskRepository;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Leave request & handover workflow (Scope.md §14). "Group" isn't a real
 * entity in this app's schema (see V17's comment on the same gap for §2.2)
 * — the suggestion ranking and pending-request scoping use the member's
 * Lead team (existing User.lead relation) as the group equivalent.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LeaveRequestService {

    private static final int HANDOVER_COMPLETE_THRESHOLD_PERCENT = 80;

    private final LeaveRequestRepository leaveRequestRepository;
    private final HandoverSuggestionRepository handoverSuggestionRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final StepRepository stepRepository;
    private final StepService stepService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final NotificationService notificationService;

    public LeaveRequest createLeaveRequest(LeaveRequest.LeaveType leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        User member = currentUser.getCurrentUser();
        LeaveRequest request = LeaveRequest.builder()
                .member(member)
                .leaveType(leaveType)
                .startDate(startDate)
                .endDate(endDate)
                .reason(reason)
                .status(LeaveRequest.Status.PENDING)
                .handoverStatus(LeaveRequest.HandoverStatus.NONE)
                .build();
        LeaveRequest saved = leaveRequestRepository.save(request);

        User freshMember = userRepository.findByIdWithLeadAndManager(member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + member.getId()));
        if (freshMember.getLead() != null) {
            notificationService.notify(freshMember.getLead(), NotificationType.LEAVE_REQUEST,
                    "Yêu cầu nghỉ phép mới",
                    freshMember.getFullName() + " xin nghỉ phép từ " + startDate + " đến " + endDate,
                    null);
        }
        return saved;
    }

    public LeaveRequest reviewLeaveRequest(Long id, boolean approve, String note) {
        if (!approve && (note == null || note.trim().isEmpty())) {
            throw new IllegalArgumentException("Lý do từ chối là bắt buộc");
        }

        LeaveRequest request = getPendingOrThrow(id);
        User reviewer = currentUser.getCurrentUser();
        validateReviewer(request, reviewer);

        request.setStatus(approve ? LeaveRequest.Status.APPROVED : LeaveRequest.Status.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(ZonedDateTime.now());
        request.setReviewNote(note);
        LeaveRequest saved = leaveRequestRepository.save(request);

        if (approve) {
            int count = generateHandoverSuggestions(saved);
            notificationService.notify(reviewer, NotificationType.HANDOVER,
                    "Gợi ý bàn giao đã sẵn sàng",
                    "Đã duyệt nghỉ phép cho " + request.getMember().getFullName() + " — " + count + " công việc cần bàn giao",
                    null);
            notificationService.notify(request.getMember(), NotificationType.LEAVE_REQUEST,
                    "Yêu cầu nghỉ phép đã được duyệt",
                    "Yêu cầu nghỉ phép của bạn từ " + request.getStartDate() + " đến " + request.getEndDate() + " đã được duyệt",
                    null);
        } else {
            notificationService.notify(request.getMember(), NotificationType.LEAVE_REQUEST,
                    "Yêu cầu nghỉ phép bị từ chối",
                    "Yêu cầu nghỉ phép của bạn đã bị từ chối: " + note,
                    null);
        }
        return saved;
    }

    /**
     * Scope.md §14.3: FAST tasks due in the window, and MULTI_STEP steps
     * with a deadline in the window, each get a suggestion pointing at the
     * lowest-workload teammate under the same Lead.
     */
    private int generateHandoverSuggestions(LeaveRequest request) {
        User member = userRepository.findByIdWithLeadAndManager(request.getMember().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getMember().getId()));
        List<User> teammates = member.getLead() != null
                ? userRepository.findByLeadId(member.getLead().getId()).stream()
                        .filter(u -> !u.getId().equals(member.getId()))
                        .collect(Collectors.toList())
                : List.of();

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        int count = 0;

        for (TaskAssignment assignment : taskAssignmentRepository.findCurrentAssignmentsForUser(member.getId())) {
            Task task = assignment.getTask();
            if (task.getTaskType().getTimeCategory() != TimeCategory.FAST) {
                continue;
            }
            if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }
            if (task.getDueDate() == null || task.getDueDate().isBefore(start) || task.getDueDate().isAfter(end)) {
                continue;
            }

            User suggested = lowestWorkload(teammates);
            handoverSuggestionRepository.save(HandoverSuggestion.builder()
                    .leaveRequest(request)
                    .task(task)
                    .entityType(HandoverSuggestion.EntityType.FAST_TASK)
                    .currentDeadline(task.getDueDate().atStartOfDay(ZoneId.systemDefault()))
                    .suggestedAssignee(suggested)
                    .action(suggested != null ? HandoverSuggestion.Action.REASSIGN : HandoverSuggestion.Action.SKIP)
                    .confirmed(false)
                    .build());
            count++;
        }

        for (Step step : stepRepository.findByAssigneeId(member.getId())) {
            if (step.getStatus() == StepStatus.DONE) {
                continue;
            }
            if (step.getDeadline() == null || step.getDeadline().toLocalDate().isBefore(start)
                    || step.getDeadline().toLocalDate().isAfter(end)) {
                continue;
            }

            User suggested = lowestWorkload(teammates);
            handoverSuggestionRepository.save(HandoverSuggestion.builder()
                    .leaveRequest(request)
                    .task(step.getSubtask().getTask())
                    .step(step)
                    .entityType(HandoverSuggestion.EntityType.STEP)
                    .currentDeadline(step.getDeadline())
                    .suggestedAssignee(suggested)
                    .action(suggested != null ? HandoverSuggestion.Action.REASSIGN : HandoverSuggestion.Action.SKIP)
                    .confirmed(false)
                    .build());
            count++;
        }

        return count;
    }

    /** Fewest currently-assigned, non-terminal tasks wins — no workload-scoring service exists to reuse. */
    private User lowestWorkload(List<User> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .min(Comparator.comparingLong(this::countActiveAssignments))
                .orElse(null);
    }

    private long countActiveAssignments(User user) {
        return taskAssignmentRepository.findCurrentAssignmentsForUser(user.getId()).stream()
                .filter(a -> a.getTask().getStatus() != TaskStatus.DONE
                        && a.getTask().getStatus() != TaskStatus.CANCELLED
                        && a.getTask().getStatus() != TaskStatus.CLOSED_LATE)
                .count();
    }

    public HandoverSuggestion confirmHandover(Long leaveRequestId, Long suggestionId,
                                               HandoverSuggestion.Action action, Long newAssigneeId) {
        LeaveRequest request = leaveRequestRepository.findByIdWithMember(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + leaveRequestId));
        User reviewer = currentUser.getCurrentUser();
        validateReviewer(request, reviewer);

        HandoverSuggestion suggestion = handoverSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover suggestion not found with id: " + suggestionId));
        if (!suggestion.getLeaveRequest().getId().equals(leaveRequestId)) {
            throw new BusinessLogicException("Gợi ý bàn giao này không thuộc yêu cầu nghỉ phép này");
        }
        if (Boolean.TRUE.equals(suggestion.getConfirmed())) {
            throw new BusinessLogicException("Gợi ý bàn giao này đã được xử lý");
        }

        User resolvedAssignee = null;
        if (action == HandoverSuggestion.Action.REASSIGN) {
            Long targetId = newAssigneeId != null ? newAssigneeId
                    : (suggestion.getSuggestedAssignee() != null ? suggestion.getSuggestedAssignee().getId() : null);
            if (targetId == null) {
                throw new IllegalArgumentException("Cần chọn người nhận bàn giao");
            }
            resolvedAssignee = userRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetId));

            if (suggestion.getEntityType() == HandoverSuggestion.EntityType.STEP) {
                stepService.assignStep(suggestion.getStep().getId(), targetId);
            } else {
                reassignFastTaskForHandover(suggestion.getTask(), resolvedAssignee, reviewer);
            }
        } else if (action == HandoverSuggestion.Action.EXTEND_DEADLINE) {
            LocalDate newDeadline = request.getEndDate().plusDays(1);
            if (suggestion.getEntityType() == HandoverSuggestion.EntityType.STEP) {
                Step step = suggestion.getStep();
                step.setDeadline(newDeadline.atStartOfDay(ZoneId.systemDefault()));
                step.setUpdatedAt(ZonedDateTime.now());
                stepRepository.save(step);
            } else {
                Task task = suggestion.getTask();
                task.setDueDate(newDeadline);
                task.setUpdatedAt(ZonedDateTime.now());
                taskRepository.save(task);
            }
        }
        // SKIP: no task change — item is just marked confirmed below.

        suggestion.setAction(action);
        suggestion.setConfirmed(true);
        suggestion.setConfirmedAssignee(resolvedAssignee);
        suggestion.setConfirmedAt(ZonedDateTime.now());
        HandoverSuggestion saved = handoverSuggestionRepository.save(suggestion);

        recomputeHandoverStatus(request);
        return saved;
    }

    /**
     * Direct reassignment for handover confirmation — deliberately doesn't
     * call TaskAssignmentService.reassignTask(), whose permission check
     * only allows the task's current assignee or a Manager. A Lead
     * confirming a handover for their own team member (already authorized
     * via validateReviewer above) is a legitimate reassigner that check
     * doesn't anticipate, and reassignTask's direction validation models
     * fresh-assignment rules that don't apply to a handover.
     */
    private void reassignFastTaskForHandover(Task task, User newAssignee, User reassignedBy) {
        taskAssignmentRepository.findCurrentAssignment(task.getId()).ifPresent(current -> {
            current.setIsCurrent(false);
            taskAssignmentRepository.saveAndFlush(current);
        });
        TaskAssignment newAssignment = TaskAssignment.builder()
                .task(task)
                .assignee(newAssignee)
                .assignedBy(reassignedBy)
                .isCurrent(true)
                .assignedAt(ZonedDateTime.now())
                .build();
        taskAssignmentRepository.saveAndFlush(newAssignment);

        notificationService.notify(newAssignee, NotificationType.TASK_ASSIGNED,
                "Công việc được bàn giao",
                "Bạn được bàn giao công việc \"" + task.getTitle() + "\"",
                task.getId());
    }

    /** Scope.md §14.4: Lead must confirm >=80% of in-window items for COMPLETE. */
    private void recomputeHandoverStatus(LeaveRequest request) {
        List<HandoverSuggestion> all = handoverSuggestionRepository.findByLeaveRequestId(request.getId());
        if (all.isEmpty()) {
            request.setHandoverStatus(LeaveRequest.HandoverStatus.COMPLETE);
        } else {
            long confirmedCount = all.stream().filter(HandoverSuggestion::getConfirmed).count();
            double percent = (confirmedCount * 100.0) / all.size();
            if (confirmedCount == 0) {
                request.setHandoverStatus(LeaveRequest.HandoverStatus.NONE);
            } else if (percent >= HANDOVER_COMPLETE_THRESHOLD_PERCENT) {
                request.setHandoverStatus(LeaveRequest.HandoverStatus.COMPLETE);
            } else {
                request.setHandoverStatus(LeaveRequest.HandoverStatus.PARTIAL);
            }
        }
        request.setUpdatedAt(ZonedDateTime.now());
        leaveRequestRepository.save(request);
    }

    private LeaveRequest getPendingOrThrow(Long id) {
        LeaveRequest request = leaveRequestRepository.findByIdWithMember(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
        if (!request.isPending()) {
            throw new ForbiddenOperationException("Yêu cầu này đã được xử lý");
        }
        return request;
    }

    /** Member's request -> their Lead or any Manager may review. */
    private void validateReviewer(LeaveRequest request, User reviewer) {
        if (reviewer.isManager()) {
            return;
        }
        User freshMember = userRepository.findByIdWithLeadAndManager(request.getMember().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getMember().getId()));
        if (reviewer.isLead() && freshMember.getLead() != null && freshMember.getLead().getId().equals(reviewer.getId())) {
            return;
        }
        throw new ForbiddenOperationException("Bạn không có quyền duyệt yêu cầu này");
    }

    @Transactional(readOnly = true)
    public LeaveRequest getLeaveRequestById(Long id) {
        return leaveRequestRepository.findByIdWithMember(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<HandoverSuggestion> getHandoverSuggestions(Long leaveRequestId) {
        return handoverSuggestionRepository.findByLeaveRequestId(leaveRequestId);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getLeaveRequestsForMember(Long memberId, int year) {
        return leaveRequestRepository.findByMemberIdAndYear(memberId, year);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingForCurrentUser() {
        User user = currentUser.getCurrentUser();
        if (user.isManager()) {
            return leaveRequestRepository.findAllPending();
        }
        if (user.isLead()) {
            return leaveRequestRepository.findPendingForLeadTeam(user.getId());
        }
        return List.of();
    }

    /** Live-computed alternatives for the API response — not persisted, so they never go stale. */
    @Transactional(readOnly = true)
    public List<Long> getAlternativeAssigneeIds(HandoverSuggestion suggestion, int limit) {
        User member = suggestion.getLeaveRequest().getMember();
        User freshMember = userRepository.findByIdWithLeadAndManager(member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + member.getId()));
        if (freshMember.getLead() == null) {
            return List.of();
        }
        Long suggestedId = suggestion.getSuggestedAssignee() != null ? suggestion.getSuggestedAssignee().getId() : null;
        return userRepository.findByLeadId(freshMember.getLead().getId()).stream()
                .filter(u -> !u.getId().equals(member.getId()))
                .filter(u -> !u.getId().equals(suggestedId))
                .sorted(Comparator.comparingLong(this::countActiveAssignments))
                .limit(limit)
                .map(User::getId)
                .collect(Collectors.toList());
    }
}
