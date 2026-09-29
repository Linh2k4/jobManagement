package com.company.jobmanagement.facade.auth;

import com.company.jobmanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for authentication flows.
 * Delegates to AuthService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFacade {

    private final AuthService authService;

    /**
     * Delegate login to auth service.
     */
    @Transactional
    public AuthService.AuthToken login(String email, String password) {
        log.info("User login attempt: {}", email);
        AuthService.AuthToken token = authService.login(email, password);
        log.info("User logged in successfully: {}", email);
        return token;
    }

    /**
     * Refresh token.
     */
    public AuthService.AuthToken refreshToken(String refreshToken) {
        log.info("Refreshing token");
        return authService.refreshToken(refreshToken);
    }
}
