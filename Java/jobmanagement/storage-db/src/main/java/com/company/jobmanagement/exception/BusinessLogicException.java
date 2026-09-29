package com.company.jobmanagement.exception;

/**
 * Exception for business logic validation failures.
 * Used when a business rule is violated (e.g., cannot create more than X items, workload exceeds limit, etc.)
 */
public class BusinessLogicException extends RuntimeException {

    public BusinessLogicException(String message) {
        super(message);
    }

    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
