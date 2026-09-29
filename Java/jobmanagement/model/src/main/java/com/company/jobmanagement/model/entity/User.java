package com.company.jobmanagement.model.entity;

import com.company.jobmanagement.model.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_role", columnList = "role"),
    @Index(name = "idx_users_manager_id", columnList = "manager_id"),
    @Index(name = "idx_users_lead_id", columnList = "lead_id"),
    @Index(name = "idx_users_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // Never serialized: this entity is returned raw by several API responses
    // (e.g. Evaluation.user) — a plain @Column here would leak the bcrypt
    // hash into those JSON bodies. getPassword() below needs the same
    // treatment since Jackson also picks up bean-style getters.
    @Column(nullable = false, length = 255)
    @JsonIgnore
    private String passwordHash;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // LAZY and un-fetched by most callers that serialize a User straight to
    // JSON (see Evaluation.user's JOIN FETCH, which only reaches this User,
    // not this User's own manager/lead) — @JsonIgnore avoids yet another
    // LazyInitializationException-on-serialize, same class of bug as
    // Group.memberships.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @JsonIgnore
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    @JsonIgnore
    private User lead;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 30)
    private String phone;

    @Column
    private LocalDate birthDate;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(role);
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public boolean isManager() {
        return role.isManager();
    }

    public boolean isLead() {
        return role.isLead();
    }

    public boolean isMember() {
        return role.isMember();
    }
}
