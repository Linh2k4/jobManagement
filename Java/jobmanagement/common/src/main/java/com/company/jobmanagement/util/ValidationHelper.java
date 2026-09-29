package com.company.jobmanagement.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Helper utility for common validation operations.
 * Provides reusable validation methods.
 */
@UtilityClass
public class ValidationHelper {

    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,128}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?1?\\d{9,15}$"
    );

    // Length constraints
    public static final int MIN_EMAIL_LENGTH = 5;
    public static final int MAX_EMAIL_LENGTH = 255;

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;

    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 100;

    /**
     * Validate email format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches() &&
                email.length() >= MIN_EMAIL_LENGTH &&
                email.length() <= MAX_EMAIL_LENGTH;
    }

    /**
     * Validate password strength.
     * Requirements: 8-128 chars, at least one uppercase, one lowercase, one digit
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validate phone number format.
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate name format (2-100 chars).
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return name.length() >= MIN_NAME_LENGTH &&
                name.length() <= MAX_NAME_LENGTH;
    }

    /**
     * Check if string is not null and not blank.
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Check if collection is not null and not empty.
     */
    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Check if string length is within bounds.
     */
    public static boolean isValidLength(String value, int minLength, int maxLength) {
        if (value == null) {
            return minLength <= 0; // null is valid if min is 0
        }
        int length = value.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validate integer is within range.
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validate long is within range.
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * Validate that ID is positive (> 0).
     */
    public static boolean isValidId(Long id) {
        return id != null && id > 0;
    }
}
