package com.company.jobmanagement.dto.response;

import com.company.jobmanagement.model.entity.HandoverSuggestion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "HandoverSuggestion", description = "One task/step affected by a leave, with a suggested reassignment")
public class HandoverSuggestionResponse {

    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long stepId;
    private String stepName;
    private HandoverSuggestion.EntityType entityType;
    private ZonedDateTime currentDeadline;
    private UserInfoResponse suggestedAssignee;
    private List<Long> alternativeAssigneeIds;
    private HandoverSuggestion.Action action;
    private Boolean confirmed;
    private UserInfoResponse confirmedAssignee;
    private ZonedDateTime confirmedAt;
}
