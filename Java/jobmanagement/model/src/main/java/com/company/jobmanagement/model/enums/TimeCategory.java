package com.company.jobmanagement.model.enums;

public enum TimeCategory {
    FAST,
    OFTEN,
    MULTI_STEP;

    public boolean isFast() {
        return this == FAST;
    }

    public boolean isOften() {
        return this == OFTEN;
    }

    public boolean isMultiStep() {
        return this == MULTI_STEP;
    }
}
