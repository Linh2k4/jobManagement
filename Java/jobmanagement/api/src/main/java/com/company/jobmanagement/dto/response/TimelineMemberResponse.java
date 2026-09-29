package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "One member row on the Timeline/Gantt view (Scope.md §5.5)")
public class TimelineMemberResponse {

    private Long userId;
    private String fullName;
    private String role;
    private List<TimelineTaskResponse> tasks;
}
