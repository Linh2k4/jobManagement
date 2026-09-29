package com.company.jobmanagement.model.enums;

public enum StepStatus {
    PENDING,
    IN_PROGRESS,
    DONE;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean isDone() {
        return this == DONE;
    }
}
