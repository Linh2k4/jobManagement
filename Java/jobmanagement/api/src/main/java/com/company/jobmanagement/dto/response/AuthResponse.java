package com.company.jobmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response containing tokens and user information.
 * Returned after successful login or token refresh.
 * Use the access_token in subsequent API calls via Authorization header.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "AuthResponse",
    description = "Authentication response with tokens and user info"
)
public class AuthResponse {

    @JsonProperty("access_token")
    @Schema(
        description = "JWT access token for API authentication. Valid for 1 hour.",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(
        description = "JWT refresh token for obtaining new access tokens. Valid for 7 days.",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String refreshToken;

    @JsonProperty("token_type")
    @Builder.Default
    @Schema(
        description = "Token type for HTTP Authorization header",
        example = "Bearer"
    )
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    @Schema(
        description = "Access token expiration time in milliseconds",
        example = "3600000"
    )
    private Long expiresIn;

    @Schema(description = "Authenticated user information")
    private UserInfoResponse user;
}
