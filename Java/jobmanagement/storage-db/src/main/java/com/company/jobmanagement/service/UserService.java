package com.company.jobmanagement.service;

import com.company.jobmanagement.model.enums.Role;
import com.company.jobmanagement.model.entity.User;
import com.company.jobmanagement.exception.BusinessLogicException;
import com.company.jobmanagement.exception.ForbiddenOperationException;
import com.company.jobmanagement.exception.ResourceNotFoundException;
import com.company.jobmanagement.repository.UserRepository;
import com.company.jobmanagement.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * User management service with password hashing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    /**
     * Register a new user with encrypted password.
     * Only Managers can register new users.
     *
     * @param email User email
     * @param password Plain password (will be hashed)
     * @param fullName User full name
     * @param role User role
     * @return Created user
     * @throws ForbiddenOperationException if current user is not Manager
     */
    public User registerUser(String email, String password, String fullName, Role role) {
        // Only Manager can register users
        User currentUserObj = currentUser.getCurrentUser();
        if (currentUserObj.getRole() != Role.MANAGER) {
            throw new ForbiddenOperationException("Only managers can register users");
        }

        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        // Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .email(email)
                .passwordHash(hashedPassword)
                .fullName(fullName)
                .role(role)
                .isActive(true)
                .createdAt(ZonedDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {} with role {}", email, role);

        return savedUser;
    }

    /**
     * Self-service password change — any authenticated user, own account only.
     * Requires the current password to match before setting the new one.
     */
    public void changePassword(String currentPassword, String newPassword) {
        User user = currentUser.getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessLogicException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed: {}", user.getEmail());
    }

    /**
     * Self-service profile update — any authenticated user, own account
     * only. Blank strings are treated as "clear this field", null means
     * "leave it unchanged" (the FE always sends the full form though).
     */
    public User updateProfile(String phone, java.time.LocalDate birthDate, String address, String bio) {
        User user = currentUser.getCurrentUser();
        user.setPhone(phone);
        user.setBirthDate(birthDate);
        user.setAddress(address);
        user.setBio(bio);
        return userRepository.save(user);
    }

    /**
     * Manager-only edit of another user's fullName/role — backs the Nhân
     * sự (HR) "Sửa" action. Self password/profile fields go through
     * changePassword/updateProfile instead; this never touches those.
     */
    public User updateUserByManager(Long userId, String fullName, Role role) {
        User currentUserObj = currentUser.getCurrentUser();
        if (currentUserObj.getRole() != Role.MANAGER) {
            throw new ForbiddenOperationException("Only managers can edit other users");
        }

        User user = getUserById(userId);
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (role != null) {
            user.setRole(role);
        }
        return userRepository.save(user);
    }

    /**
     * List every user (Manager only) — backs the Nhân sự (HR) admin screen.
     */
    public List<User> listAllUsers() {
        User currentUserObj = currentUser.getCurrentUser();
        if (currentUserObj.getRole() != Role.MANAGER && currentUserObj.getRole() != Role.LEAD) {
            throw new ForbiddenOperationException("Only managers and leads can list users");
        }
        return userRepository.findAll();
    }

    /**
     * Get user by ID.
     *
     * @param userId User ID
     * @return User if found
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    /**
     * Get user by email.
     *
     * @param email User email
     * @return User if found
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Deactivate a user account.
     * Only Managers can deactivate users.
     *
     * @param userId User ID to deactivate
     * @throws ForbiddenOperationException if not a Manager
     */
    public void deactivateUser(Long userId) {
        User currentUserObj = currentUser.getCurrentUser();
        if (currentUserObj.getRole() != Role.MANAGER) {
            throw new ForbiddenOperationException("Only managers can deactivate users");
        }

        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated: {}", user.getEmail());
    }

    /**
     * Reactivate a previously deactivated user account.
     * Only Managers can activate users.
     *
     * @param userId User ID to activate
     * @throws ForbiddenOperationException if not a Manager
     */
    public void activateUser(Long userId) {
        User currentUserObj = currentUser.getCurrentUser();
        if (currentUserObj.getRole() != Role.MANAGER) {
            throw new ForbiddenOperationException("Only managers can activate users");
        }

        User user = getUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);

        log.info("User activated: {}", user.getEmail());
    }
}
