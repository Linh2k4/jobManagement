package com.company.jobmanagement.controller.task;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.TaskAttachmentResponse;
import com.company.jobmanagement.model.entity.TaskAttachment;
import com.company.jobmanagement.service.TaskAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Task Attachments", description = "Files attached to a task, stored in MinIO")
public class TaskAttachmentController extends BaseController {

    private final TaskAttachmentService taskAttachmentService;

    @GetMapping
    @Operation(summary = "List task attachments", operationId = "getTaskAttachments")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<List<TaskAttachmentResponse>>> getAttachments(@PathVariable Long taskId) {
        List<TaskAttachmentResponse> responses = taskAttachmentService.getAttachments(taskId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(responses));
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Upload a file to a task", operationId = "uploadTaskAttachment")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<TaskAttachmentResponse>> upload(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file) {
        try {
            TaskAttachment attachment = taskAttachmentService.upload(
                    taskId, file.getOriginalFilename(), file.getBytes(), file.getContentType());
            return created(ApiResponse.success("File uploaded", toDTO(attachment)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Download an attachment", operationId = "downloadTaskAttachment")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId) {
        TaskAttachment attachment = taskAttachmentService.getAttachment(taskId, attachmentId);
        Resource resource = new InputStreamResource(taskAttachmentService.download(taskId, attachmentId));
        return download(resource, attachment.getFileName());
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Delete an attachment", operationId = "deleteTaskAttachment")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<?>> deleteAttachment(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId) {
        taskAttachmentService.delete(taskId, attachmentId);
        return ok(ApiResponse.success("Attachment deleted"));
    }

    private TaskAttachmentResponse toDTO(TaskAttachment attachment) {
        return TaskAttachmentResponse.builder()
                .id(attachment.getId())
                .uploadedById(attachment.getUploadedBy().getId())
                .uploadedByName(attachment.getUploadedBy().getFullName())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
