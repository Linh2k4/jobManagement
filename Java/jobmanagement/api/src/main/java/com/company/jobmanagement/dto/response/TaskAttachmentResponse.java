package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "File attached to a task")
public class TaskAttachmentResponse {

    private Long id;
    private Long uploadedById;
    private String uploadedByName;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private ZonedDateTime createdAt;
}
