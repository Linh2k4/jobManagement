package com.company.jobmanagement.model.enums;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    CLOSED_LATE,  // Completed after deadline
    CANCELLED;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    public boolean isDone() {
        return this == DONE;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    public boolean isTerminal() {
        return this == DONE || this == CANCELLED;
    }
}
