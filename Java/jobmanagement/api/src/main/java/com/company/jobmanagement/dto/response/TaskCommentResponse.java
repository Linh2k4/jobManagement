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
@Schema(description = "Comment on a task")
public class TaskCommentResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String content;
    private ZonedDateTime createdAt;
}
