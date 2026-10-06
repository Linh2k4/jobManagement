package com.company.jobmanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User registration request with password validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "RegisterUserRequest",
    description = "Request to register a new user"
)
public class RegisterUserRequest {

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(
        description = "User email (must be unique)",
        example = "newuser@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    @Schema(
        description = "User full name",
        example = "John Doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String fullName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 128, message = "Mật khẩu phải có từ 6 đến 128 ký tự")
    @Schema(
        description = "Mật khẩu (tối thiểu 6 ký tự)",
        example = "password123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    @Schema(
        description = "User role (MANAGER, LEAD, MEMBER)",
        example = "MEMBER",
        allowableValues = {"MANAGER", "LEAD", "MEMBER"}
    )
    private String role;
}
