package com.company.jobmanagement.model.enums;

public enum OrganizeBy {
    SECTION,
    MONTH;

    public boolean isSection() {
        return this == SECTION;
    }

    public boolean isMonth() {
        return this == MONTH;
    }
}
