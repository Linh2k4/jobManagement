package com.company.jobmanagement.constant;

import lombok.experimental.UtilityClass;

/**
 * Application-wide constants.
 * Avoid magic numbers and strings throughout the codebase.
 */
@UtilityClass
public class AppConstants {

    // ===== Security Constants =====
    public static final String AUTH_HEADER_PREFIX = "Bearer ";
    public static final String JWT_CLAIM_USER_ID = "userId";
    public static final String JWT_CLAIM_EMAIL = "email";
    public static final String JWT_CLAIM_ROLE = "role";

    public static final long JWT_ACCESS_TOKEN_EXPIRATION_MS = 3_600_000L;      // 1 hour
    public static final long JWT_REFRESH_TOKEN_EXPIRATION_MS = 604_800_000L;   // 7 days

    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;

    // ===== API Constants =====
    public static final String API_VERSION = "/api/v1";
    public static final String SWAGGER_TITLE = "Job Management API";
    public static final String SWAGGER_VERSION = "1.0.0";
    public static final String SWAGGER_DESCRIPTION = "Role-based work/task management system with evaluation and KPI tracking";

    // ===== Pagination Constants =====
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MIN_PAGE_SIZE = 1;

    // ===== Validation Constants =====
    public static final int MIN_EMAIL_LENGTH = 5;
    public static final int MAX_EMAIL_LENGTH = 255;

    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 100;

    public static final int MIN_TITLE_LENGTH = 3;
    public static final int MAX_TITLE_LENGTH = 255;

    public static final int MIN_DESCRIPTION_LENGTH = 0;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    // ===== Estimation Constants =====
    public static final int MIN_ESTIMATE_MINUTES = 1;
    public static final int MAX_ESTIMATE_MINUTES = 480 * 5;  // 5 days = 2400 minutes

    public static final long MIN_TASK_ID = 1L;

    // ===== KPI Constants =====
    public static final double SELF_EVALUATION_WEIGHT = 0.2;
    public static final double LEAD_EVALUATION_WEIGHT = 0.3;
    public static final double COMPLETION_RATE_WEIGHT = 0.5;

    // ===== Time Constants =====
    public static final int MINUTES_PER_HOUR = 60;
    public static final int HOURS_PER_DAY = 8;
    public static final int DAYS_PER_WEEK = 5;
    public static final int WEEKS_PER_MONTH = 4;

    // ===== Error Messages =====
    public static final String ERROR_UNAUTHORIZED = "Unauthorized access";
    public static final String ERROR_FORBIDDEN = "Forbidden access";
    public static final String ERROR_NOT_FOUND = "Resource not found";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed";
    public static final String ERROR_DUPLICATE_ENTRY = "Duplicate entry";

    // ===== Success Messages =====
    public static final String SUCCESS_CREATED = "Resource created successfully";
    public static final String SUCCESS_UPDATED = "Resource updated successfully";
    public static final String SUCCESS_DELETED = "Resource deleted successfully";
    public static final String SUCCESS_OPERATION = "Operation completed successfully";

    // ===== Audit Constants =====
    public static final String AUDIT_ACTION_CREATE = "CREATE";
    public static final String AUDIT_ACTION_UPDATE = "UPDATE";
    public static final String AUDIT_ACTION_DELETE = "DELETE";
    public static final String AUDIT_ACTION_LOGIN = "LOGIN";
    public static final String AUDIT_ACTION_LOGOUT = "LOGOUT";
    public static final String AUDIT_ACTION_FAILED_LOGIN = "FAILED_LOGIN";

    // ===== Cache Constants =====
    public static final long CACHE_TTL_MINUTES = 30;
    public static final String CACHE_KEY_USER = "user:";
    public static final String CACHE_KEY_TASK = "task:";
}
