package com.company.jobmanagement.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist service.
 * In production, use Redis for distributed caching.
 *
 * For production, replace with:
 * - RedisTemplate: distributed cache across servers
 * - TTL managed by Redis (auto-cleanup expired tokens)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {

    // In-memory blacklist (for single-instance deployment)
    // For multi-instance: use Redis or similar
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    /**
     * Add token to blacklist (invalidate token).
     * Call this when user logs out or token needs revocation.
     *
     * @param token JWT token to blacklist
     */
    public void addToBlacklist(String token) {
        blacklist.add(token);
        log.debug("Token added to blacklist");
    }

    /**
     * Check if token is blacklisted.
     *
     * @param token JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }

    /**
     * Remove token from blacklist (cleanup, usually done by TTL expiration).
     *
     * @param token JWT token to remove
     */
    public void removeFromBlacklist(String token) {
        blacklist.remove(token);
    }

    /**
     * Get current blacklist size (for monitoring).
     */
    public int getBlacklistSize() {
        return blacklist.size();
    }

    /**
     * Clear all blacklisted tokens (admin use only).
     */
    public void clearBlacklist() {
        blacklist.clear();
        log.warn("Token blacklist cleared");
    }
}
