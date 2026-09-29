package com.company.jobmanagement.controller.auth;

import com.company.jobmanagement.controller.BaseController;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.dto.request.LoginRequest;
import com.company.jobmanagement.dto.request.RefreshTokenRequest;
import com.company.jobmanagement.dto.response.ApiResponse;
import com.company.jobmanagement.dto.response.AuthResponse;
import com.company.jobmanagement.dto.response.UserInfoResponse;
import com.company.jobmanagement.exception.ErrorResponse;
import com.company.jobmanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller handling user login, token refresh, and logout.
 * <p>
 * Provides JWT-based authentication endpoints:
 * 1. POST /login - Authenticate and receive tokens
 * 2. POST /refresh - Refresh expired access token
 * 3. POST /logout - Clear session
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
    name = "Authentication",
    description = "User authentication endpoints (JWT-based)\n\n" +
        "## How to Authenticate\n" +
        "1. Call `/login` with email and password\n" +
        "2. Receive `access_token` and `refresh_token`\n" +
        "3. Use `access_token` in Authorization header: `Bearer <token>`\n" +
        "4. When access token expires, use `/refresh` with `refresh_token`\n" +
        "5. Call `/logout` to clear session"
)
public class AuthController extends BaseController {

    private final AuthService authService;

    /**
     * Authenticates user with email and password credentials.
     * <p>
     * Returns JWT access token (1 hour) and refresh token (7 days).
     * Use access token in Authorization header: Bearer <token>
     * </p>
     *
     * @param loginRequest contains email and password
     * @return AuthResponse with tokens and user information
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login with credentials",
        description = "Authenticate user and receive JWT tokens (access: 1h, refresh: 7d)",
        operationId = "login"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid email or password format",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthService.AuthToken authToken = authService.login(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        );
        AuthResponse response = buildAuthResponse(authToken);
        return ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refreshes access token using a valid refresh token.
     * <p>
     * Call this when access token is about to expire (typically at 50% TTL).
     * Refresh token is valid for 7 days.
     * </p>
     *
     * @param refreshTokenRequest contains refresh token
     * @return AuthResponse with new tokens
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Generate new access token using valid refresh token",
        operationId = "refreshToken"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token refresh successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid or missing refresh token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Refresh token expired or invalid",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthService.AuthToken authToken = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        AuthResponse response = buildAuthResponse(authToken);
        return ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * Logs out the current user by clearing the security context.
     * <p>
     * The client should discard tokens after logout and clear local storage.
     * Token is added to blacklist and becomes invalid immediately.
     * </p>
     *
     * @param authorizationHeader Bearer token from Authorization header
     * @return 204 No Content
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Clear user session and revoke token (add to blacklist)",
        operationId = "logout"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Logout successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - no valid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Void> logout(
        @RequestHeader(value = "Authorization", required = false)
        String authorizationHeader
    ) {
        // Extract token from Authorization header
        String token = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        }

        authService.logout(token);
        return noContent();
    }

    /**
     * Builds AuthResponse DTO from internal AuthToken object.
     *
     * @param authToken containing access token, refresh token, and user
     * @return AuthResponse ready for API response
     */
    private AuthResponse buildAuthResponse(AuthService.AuthToken authToken) {
        User user = authToken.getUser();
        return AuthResponse.builder()
                .accessToken(authToken.getAccessToken())
                .refreshToken(authToken.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .user(UserInfoResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
