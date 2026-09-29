package com.company.jobmanagement.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Standard API error response wrapper for all error scenarios.
 * <p>
 * Used to provide consistent error format across the API:
 * - status: HTTP status code (e.g., 400, 404, 500)
 * - message: human-readable error description
 * - path: the API endpoint that caused the error
 * - timestamp: when the error occurred (ISO-8601)
 * - errors: field-level validation errors (if applicable, HTTP 400 only)
 * </p>
 * <p>
 * Error Response Examples:
 * <pre>
 * // Validation error (HTTP 400)
 * {
 *   "status": 400,
 *   "message": "Validation failed",
 *   "path": "/api/v1/tasks",
 *   "timestamp": "2026-06-29T10:30:00+07:00",
 *   "errors": {
 *     "title": "Title is required",
 *     "priority": "Priority must be 1-5"
 *   }
 * }
 *
 * // Not found error (HTTP 404)
 * {
 *   "status": 404,
 *   "message": "Task not found",
 *   "path": "/api/v1/tasks/999",
 *   "timestamp": "2026-06-29T10:30:00+07:00"
 * }
 * </pre>
 * </p>
 *
 * @author Khánh VD
 * @since 2026-06-29
 * @see ApiResponse for success responses
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "ErrorResponse",
    description = "Standard error response returned by API"
)
public class ErrorResponse {

    @Schema(
        description = "HTTP status code",
        example = "400"
    )
    private int status;

    @Schema(
        description = "Error message describing the issue",
        example = "Validation failed"
    )
    private String message;

    @Schema(
        description = "Request path that caused the error",
        example = "/api/v1/auth/login"
    )
    private String path;

    @Schema(
        description = "Timestamp when error occurred (ISO 8601 format)",
        example = "2026-06-23T10:30:00+07:00"
    )
    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

    @Schema(
        description = "Field-level validation errors (present if status is 400)",
        example = "{\"email\": \"Email should be valid\", \"password\": \"Password is required\"}"
    )
    private Map<String, String> errors;

}
