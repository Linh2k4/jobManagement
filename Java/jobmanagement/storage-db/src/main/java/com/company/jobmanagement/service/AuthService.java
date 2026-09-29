package com.company.jobmanagement.service;

import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.JwtTokenProvider;
import com.company.jobmanagement.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthToken login(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name(),
                    user.getRole().name()
            );

            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getId(),
                    user.getEmail()
            );

            return AuthToken.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(user)
                    .build();
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password", e);
        }
    }

    public AuthToken refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        Long userId = jwtTokenProvider.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.getIsActive()) {
            throw new BadCredentialsException("User account is disabled");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getRole().name()
        );

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getEmail()
        );

        return AuthToken.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(user)
                .build();
    }

    /**
     * Logout user by:
     * 1. Clearing security context (client-side)
     * 2. Adding current token to blacklist (server-side revocation)
     *
     * @param token Current access token to revoke
     */
    public void logout(String token) {
        SecurityContextHolder.clearContext();

        // Add token to blacklist so it can't be used again
        if (StringUtils.hasText(token)) {
            tokenBlacklistService.addToBlacklist(token);
            log.info("User logged out, token revoked");
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class AuthToken {
        private String accessToken;
        private String refreshToken;
        private User user;
    }
}
