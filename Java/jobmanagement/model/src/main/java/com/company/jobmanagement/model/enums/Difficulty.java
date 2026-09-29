package com.company.jobmanagement.model.enums;

import lombok.Getter;

/**
 * Task difficulty levels (1-5 scale).
 * Set by Lead or Manager, NOT by team members.
 * Used in KPI weight calculation.
 */
@Getter
public enum Difficulty {
    VERY_EASY(1, "Rất dễ", "Thao tác thuần tuý, không cần phán đoán"),
    EASY(2, "Dễ", "Quen thuộc, ít biến số"),
    MEDIUM(3, "Trung bình", "Cần xử lý thông tin, có quyết định nhỏ"),
    HARD(4, "Khó", "Phức tạp, nhiều bên liên quan, rủi ro"),
    VERY_HARD(5, "Rất khó", "Đòi hỏi chuyên môn cao, ảnh hưởng lớn");

    private final int level;
    private final String displayName;
    private final String description;

    Difficulty(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    /** The FE sends/expects the 1-5 numeric level, not the enum constant name. */
    public static Difficulty fromLevel(int level) {
        for (Difficulty d : values()) {
            if (d.level == level) {
                return d;
            }
        }
        throw new IllegalArgumentException("No Difficulty with level: " + level);
    }
}
