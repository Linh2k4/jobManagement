package com.company.jobmanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Standard API success response wrapper for all endpoints.
 * <p>
 * Used to provide consistent response format across the API:
 * - success: boolean indicating operation success (true for this class)
 * - message: human-readable description of the result
 * - data: the actual response payload
 * - timestamp: when the response was created (ISO-8601)
 * - requestId: unique identifier for tracing (optional)
 * </p>
 * <p>
 * Example response:
 * <pre>
 * {
 *   "success": true,
 *   "message": "User created successfully",
 *   "data": {"id": 123, "email": "user@company.com"},
 *   "timestamp": "2026-06-29T10:30:00+07:00",
 *   "requestId": "550e8400-e29b-41d4-a716-446655440000"
 * }
 * </pre>
 * </p>
 *
 * @param <T> the response data type
 * @author Khánh VD
 * @since 2026-06-29
 * @see com.company.jobmanagement.exception.ErrorResponse for error responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    description = "Standard API response wrapper"
)
public class ApiResponse<T> {

    @Schema(
        description = "Response success status",
        example = "true"
    )
    private boolean success;

    @Schema(
        description = "Response message",
        example = "Operation successful"
    )
    private String message;

    @Schema(
        description = "Response data"
    )
    private T data;

    @Schema(
        description = "Response timestamp (ISO 8601)",
        example = "2026-06-24T10:30:00+07:00"
    )
    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

    @Schema(
        description = "Request ID for tracing",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String requestId;

    /**
     * Create success response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    /**
     * Create success response with custom message.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Create success response without data.
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Create error response.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * Create error response with data.
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }
}
