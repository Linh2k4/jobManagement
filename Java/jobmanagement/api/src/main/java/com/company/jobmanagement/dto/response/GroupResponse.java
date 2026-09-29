package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Group", description = "A Lead-managed team a Member can belong to (Scope.md §13)")
public class GroupResponse {
    private Long id;
    private String name;
    private String description;
    private UserInfoResponse lead;
    private Integer memberCount;
}
