package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskComment;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.repository.TaskCommentRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskService taskService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<TaskComment> getComments(Long taskId) {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public TaskComment addComment(Long taskId, String content) {
        Task task = taskService.getTask(taskId);
        User user = currentUser.getCurrentUser();

        TaskComment comment = TaskComment.builder()
                .task(task)
                .user(user)
                .content(content)
                .build();

        return taskCommentRepository.save(comment);
    }

    public void deleteComment(Long taskId, Long commentId) {
        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (!comment.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("Comment not found with id: " + commentId);
        }

        User user = currentUser.getCurrentUser();
        if (!comment.getUser().getId().equals(user.getId()) && !user.isManager()) {
            throw new com.company.jobmanagement.exception.ForbiddenOperationException(
                    "Only the comment author or Manager can delete this comment");
        }

        taskCommentRepository.delete(comment);
    }
}
