package com.company.jobmanagement.model.enums;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    MANAGER("ROLE_MANAGER"),
    LEAD("ROLE_LEAD"),
    MEMBER("ROLE_MEMBER");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    public boolean isManager() {
        return this == MANAGER;
    }

    public boolean isLead() {
        return this == LEAD;
    }

    public boolean isMember() {
        return this == MEMBER;
    }
}
