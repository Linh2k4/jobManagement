package com.company.jobmanagement.controller.user;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.model.entity.Group;
import com.company.jobmanagement.model.entity.GroupMembership;
import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.dto.request.ChangePasswordRequest;
import com.company.jobmanagement.dto.request.RegisterUserRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.exception.ErrorResponse;
import com.company.jobmanagement.repository.GroupMembershipRepository;
import com.company.jobmanagement.repository.GroupRepository;
import com.company.jobmanagement.security.CurrentUser;
import com.company.jobmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User management controller handling registration and user lifecycle.
 * <p>
 * Provides endpoints for:
 * - User registration (Manager only)
 * - Retrieve current user info (Authenticated)
 * - Get user by ID (Manager only)
 * - Deactivate user (Manager only)
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(
    name = "Users",
    description = "User management endpoints\n\n" +
        "## Permissions\n" +
        "- POST (register): Manager only\n" +
        "- GET /me: Authenticated\n" +
        "- DELETE: Manager only"
)
public class UserController extends BaseController {

    private final UserService userService;
    private final CurrentUser currentUser;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupRepository groupRepository;

    /**
     * List every user (Manager only) — backs the Nhân sự (HR) admin screen.
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "List all users",
        description = "Retrieve every user account (Manager only)",
        operationId = "listUsers",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<List<UserInfoResponse>>> listUsers() {
        List<UserInfoResponse> users = userService.listAllUsers().stream()
                .map(this::mapToUserInfoResponse)
                .collect(Collectors.toList());
        return ok(ApiResponse.success(users));
    }

    /**
     * Register a new user (Manager only).
     * Password is automatically hashed using BCrypt.
     *
     * @param request Registration request with email, password, name
     * @return Created user info
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Register new user",
        description = "Create a new user account (Manager only). Password is hashed automatically.",
        operationId = "registerUser",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or duplicate email"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden (not a Manager)"
        )
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        Role role = Role.valueOf(request.getRole() != null ? request.getRole() : "MEMBER");

        User user = userService.registerUser(
            request.getEmail(),
            request.getPassword(),
            request.getFullName(),
            role
        );

        UserInfoResponse response = mapToUserInfoResponse(user);
        return created(ApiResponse.success("User registered successfully", response));
    }

    /**
     * Self-service password change (any authenticated user, own account).
     */
    @PutMapping("/me/password")
    @Operation(
        summary = "Change my password",
        description = "Change the authenticated user's own password (requires current password)",
        operationId = "changePassword",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<?>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getCurrentPassword(), request.getNewPassword());
        return ok(ApiResponse.success("Password changed successfully"));
    }

    /**
     * Self-service profile update (any authenticated user, own account).
     */
    @PutMapping("/me")
    @Operation(
        summary = "Update my profile",
        description = "Update the authenticated user's own profile fields (phone/birthDate/address/bio)",
        operationId = "updateProfile",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateProfile(@RequestBody com.company.jobmanagement.dto.request.UpdateProfileRequest request) {
        User user = userService.updateProfile(request.getPhone(), request.getBirthDate(), request.getAddress(), request.getBio());
        return ok(ApiResponse.success(mapToUserInfoResponse(user)));
    }

    /**
     * Get current user info.
     *
     * @return Current authenticated user
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user",
        description = "Get authenticated user information",
        operationId = "getCurrentUser",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User info retrieved"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser() {
        User user = currentUser.getCurrentUser();
        return ok(ApiResponse.success(mapToUserInfoResponse(user)));
    }

    /**
     * Get user by ID (Manager only).
     *
     * @param userId User ID
     * @return User information
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve specific user details (Manager only)",
        operationId = "getUserById",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden (not a Manager)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ok(ApiResponse.success(mapToUserInfoResponse(user)));
    }

    /**
     * Edit another user's fullName/role (Manager only) — backs the Nhân
     * sự "Sửa" action.
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Update user",
        description = "Update another user's fullName/role (Manager only)",
        operationId = "updateUser",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateUser(
            @PathVariable Long userId,
            @RequestBody com.company.jobmanagement.dto.request.UpdateUserRequest request) {
        Role role = request.getRole() != null ? Role.valueOf(request.getRole()) : null;
        User user = userService.updateUserByManager(userId, request.getFullName(), role);
        return ok(ApiResponse.success(mapToUserInfoResponse(user)));
    }

    /**
     * Deactivate a user (Manager only).
     *
     * @param userId User ID to deactivate
     * @return 204 No Content
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Deactivate user",
        description = "Deactivate a user account (Manager only)",
        operationId = "deactivateUser",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "User deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden (not a Manager)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return noContent();
    }

    /**
     * Reactivate a previously deactivated user (Manager only).
     */
    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Activate user",
        description = "Reactivate a deactivated user account (Manager only)",
        operationId = "activateUser",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    public ResponseEntity<ApiResponse<?>> activateUser(@PathVariable Long userId) {
        userService.activateUser(userId);
        return ok(ApiResponse.success("User activated"));
    }

    /**
     * Map User entity to UserInfoResponse DTO, including the group name
     * (Scope.md §13): a Member's primary group, a Lead's led group(s), or
     * null for a Manager.
     */
    private UserInfoResponse mapToUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .groupName(resolveGroupName(user))
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .address(user.getAddress())
                .bio(user.getBio())
                .build();
    }

    private String resolveGroupName(User user) {
        if (user.getRole() == Role.MEMBER) {
            List<GroupMembership> memberships = groupMembershipRepository.findByMemberId(user.getId());
            return memberships.stream()
                    .filter(GroupMembership::getIsPrimary)
                    .findFirst()
                    .or(() -> memberships.stream().findFirst())
                    .map(gm -> gm.getGroup().getName())
                    .orElse(null);
        }
        if (user.getRole() == Role.LEAD) {
            List<Group> led = groupRepository.findByLeadId(user.getId());
            if (led.isEmpty()) return null;
            return led.stream().map(Group::getName).collect(Collectors.joining(", "));
        }
        return null;
    }
}
