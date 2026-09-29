package com.company.jobmanagement.service;

import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.model.entity.Task;
import com.company.jobmanagement.model.entity.TaskAttachment;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.repository.TaskAttachmentRepository;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.storage.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskAttachmentService {

    private final TaskAttachmentRepository taskAttachmentRepository;
    private final TaskService taskService;
    private final CurrentUser currentUser;
    private final MinioStorageService storageService;

    @Transactional(readOnly = true)
    public List<TaskAttachment> getAttachments(Long taskId) {
        return taskAttachmentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    public TaskAttachment upload(Long taskId, String fileName, byte[] content, String contentType) {
        Task task = taskService.getTask(taskId);
        User user = currentUser.getCurrentUser();

        String objectKey = "tasks/" + taskId + "/" + UUID.randomUUID() + "-" + fileName;
        storageService.upload(objectKey, content, contentType);

        TaskAttachment attachment = TaskAttachment.builder()
                .task(task)
                .uploadedBy(user)
                .fileName(fileName)
                .objectKey(objectKey)
                .contentType(contentType)
                .fileSize((long) content.length)
                .build();

        return taskAttachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public TaskAttachment getAttachment(Long taskId, Long attachmentId) {
        TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));
        if (!attachment.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("Attachment not found with id: " + attachmentId);
        }
        return attachment;
    }

    public InputStream download(Long taskId, Long attachmentId) {
        TaskAttachment attachment = getAttachment(taskId, attachmentId);
        return storageService.download(attachment.getObjectKey());
    }

    public void delete(Long taskId, Long attachmentId) {
        TaskAttachment attachment = getAttachment(taskId, attachmentId);

        User user = currentUser.getCurrentUser();
        if (!attachment.getUploadedBy().getId().equals(user.getId()) && !user.isManager()) {
            throw new ForbiddenOperationException("Only the uploader or Manager can delete this attachment");
        }

        storageService.delete(attachment.getObjectKey());
        taskAttachmentRepository.delete(attachment);
    }
}
