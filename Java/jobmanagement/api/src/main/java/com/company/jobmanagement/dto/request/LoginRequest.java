package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User login request containing email and password credentials.
 *
 * @example
 * {
 *   "email": "user@example.com",
 *   "password": "password123"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "LoginRequest",
    description = "Request object for user authentication",
    example = "{\"email\": \"manager@example.com\", \"password\": \"password123\"}"
)
public class LoginRequest {

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Schema(
        description = "User email address",
        example = "manager@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(
        description = "User password",
        example = "password123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;
}
