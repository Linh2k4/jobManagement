package com.company.jobmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * User information in authentication and profile responses.
 * Contains basic user details and role information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UserInfo", description = "User information with role details")
public class UserInfoResponse {

    @Schema(
        description = "Unique user identifier",
        example = "1"
    )
    private Long id;

    @Schema(
        description = "User email address (unique identifier)",
        example = "manager@example.com"
    )
    private String email;

    @Schema(
        description = "User full name",
        example = "John Manager"
    )
    private String fullName;

    @Schema(
        description = "User role (MANAGER, LEAD, or MEMBER)",
        example = "MANAGER",
        allowableValues = {"MANAGER", "LEAD", "MEMBER"}
    )
    private String role;

    @Schema(description = "Whether the account is active (false = deactivated)")
    private Boolean isActive;

    @Schema(description = "Account creation timestamp")
    private ZonedDateTime createdAt;

    @Schema(description = "Group name: primary group for a MEMBER, led group(s) for a LEAD, null for a MANAGER")
    private String groupName;

    private String phone;
    private LocalDate birthDate;
    private String address;
    private String bio;
}
