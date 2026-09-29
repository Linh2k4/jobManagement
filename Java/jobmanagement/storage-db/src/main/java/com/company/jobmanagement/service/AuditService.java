package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.User;

/**
 * Service for auditing user actions.
 * Tracks who did what and when.
 */
public interface AuditService {

    /**
     * Log user action.
     *
     * @param user User who performed action
     * @param action Action performed (e.g., "CREATE_TASK", "UPDATE_TASK")
     * @param entityType Entity type (e.g., "Task", "User")
     * @param entityId Entity ID
     * @param details Additional details (JSON)
     */
    void logAction(User user, String action, String entityType, Long entityId, String details);

    /**
     * Log user login.
     */
    void logLogin(User user);

    /**
     * Log user logout.
     */
    void logLogout(User user);

    /**
     * Log failed login attempt.
     */
    void logFailedLogin(String email);

    /**
     * Get audit trail for entity.
     *
     * @param entityType Entity type
     * @param entityId Entity ID
     * @return Audit trail (JSON array)
     */
    String getAuditTrail(String entityType, Long entityId);
}
