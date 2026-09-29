package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Manager-only edit of another user's fullName/role (Nhân sự "Sửa" action).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Update another user's fullName/role (Manager only)")
public class UpdateUserRequest {

    private String fullName;

    @Schema(allowableValues = {"MANAGER", "LEAD", "MEMBER"})
    private String role;
}
