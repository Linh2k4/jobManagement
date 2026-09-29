package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskPrivateNote;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.repository.TaskPrivateNoteRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskPrivateNoteService {

    private final TaskPrivateNoteRepository taskPrivateNoteRepository;
    private final TaskService taskService;
    private final CurrentUser currentUser;

    /** Returns "" if the current user has never saved a note for this task. */
    @Transactional(readOnly = true)
    public String getMyNotes(Long taskId) {
        Long userId = currentUser.getCurrentUserId();
        return taskPrivateNoteRepository.findByTaskIdAndUserId(taskId, userId)
                .map(TaskPrivateNote::getNotes)
                .orElse("");
    }

    public void updateMyNotes(Long taskId, String notes) {
        Long userId = currentUser.getCurrentUserId();
        TaskPrivateNote note = taskPrivateNoteRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseGet(() -> {
                    Task task = taskService.getTask(taskId);
                    User user = currentUser.getCurrentUser();
                    return TaskPrivateNote.builder().task(task).user(user).notes("").build();
                });

        note.setNotes(notes);
        note.setUpdatedAt(ZonedDateTime.now());
        taskPrivateNoteRepository.save(note);
    }
}
