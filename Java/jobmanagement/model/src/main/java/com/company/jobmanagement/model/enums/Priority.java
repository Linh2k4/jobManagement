package com.company.jobmanagement.model.enums;

import lombok.Getter;

/**
 * Task priority levels used in KPI weight calculation.
 * Higher priority multiplies task weight in KPI formula.
 */
@Getter
public enum Priority {
    LOW(0.8, "Thấp"),
    MEDIUM(1.0, "Trung bình"),
    HIGH(1.2, "Cao"),
    URGENT(1.5, "Khẩn cấp");

    private final double multiplier;
    private final String displayName;

    Priority(double multiplier, String displayName) {
        this.multiplier = multiplier;
        this.displayName = displayName;
    }
}
