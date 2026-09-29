package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.AuditLog;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void logAction(User user, String action, String entityType, Long entityId, String details) {
        try {
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .status("SUCCESS")
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} performed {} on {}/{}", 
                    user.getEmail(), action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to log action: {} {} {} {}", action, entityType, entityId, e.getMessage());
        }
    }

    @Override
    public void logLogin(User user) {
        logAction(user, "LOGIN", "USER", user.getId(), "User logged in");
    }

    @Override
    public void logLogout(User user) {
        logAction(user, "LOGOUT", "USER", user.getId(), "User logged out");
    }

    @Override
    public void logFailedLogin(String email) {
        try {
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();

            AuditLog auditLog = AuditLog.builder()
                    .action("FAILED_LOGIN")
                    .entityType("USER")
                    .entityId(-1L)
                    .details("Failed login attempt for email: " + email)
                    .status("FAILED")
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.warn("Failed login attempt for: {} from IP: {}", email, ipAddress);
        } catch (Exception e) {
            log.error("Failed to log failed login: {}", e.getMessage());
        }
    }

    @Override
    public String getAuditTrail(String entityType, Long entityId) {
        try {
            List<AuditLog> logs = auditLogRepository.findByEntity(entityType, entityId);
            return objectMapper.writeValueAsString(logs);
        } catch (Exception e) {
            log.error("Failed to retrieve audit trail: {}", e.getMessage());
            return "[]";
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine client IP address: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not determine user agent: {}", e.getMessage());
        }
        return "UNKNOWN";
    }
}
