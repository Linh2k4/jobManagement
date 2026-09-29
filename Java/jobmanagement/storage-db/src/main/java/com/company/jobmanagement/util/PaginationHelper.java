package com.company.jobmanagement.util;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Helper utility for pagination operations.
 * Provides default and validated pagination parameters.
 */
@UtilityClass
public class PaginationHelper {

    // Default pagination constants
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final int MIN_SIZE = 1;

    /**
     * Create pageable with default values.
     *
     * @return Pageable with default page=0, size=20
     */
    public static Pageable getDefaultPageable() {
        return PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    /**
     * Create pageable with custom page and size.
     * Size is validated to be within min/max bounds.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Validated Pageable
     */
    public static Pageable getPageable(int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.max(MIN_SIZE, Math.min(size, MAX_SIZE));
        return PageRequest.of(validPage, validSize);
    }

    /**
     * Create pageable with custom page, size, and sort.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sort Sort order
     * @return Validated Pageable
     */
    public static Pageable getPageable(int page, int size, Sort sort) {
        int validPage = Math.max(0, page);
        int validSize = Math.max(MIN_SIZE, Math.min(size, MAX_SIZE));
        return PageRequest.of(validPage, validSize, sort);
    }

    /**
     * Create pageable with sort by single field.
     *
     * @param page Page number
     * @param size Page size
     * @param sortBy Field name to sort by
     * @param ascending Sort direction
     * @return Validated Pageable
     */
    public static Pageable getPageable(int page, int size, String sortBy, boolean ascending) {
        Sort sort = ascending
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return getPageable(page, size, sort);
    }

    /**
     * Check if page size is valid.
     */
    public static boolean isValidSize(int size) {
        return size >= MIN_SIZE && size <= MAX_SIZE;
    }
}
