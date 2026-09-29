--liquibase formatted sql

--changeset jobmanagement:V1

-- Create enum type for user roles
CREATE TYPE user_role AS ENUM ('MANAGER', 'LEAD', 'MEMBER');

-- Users table with hierarchical role structure
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    manager_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    lead_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for common queries
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_users_lead_id ON users(lead_id);
CREATE INDEX idx_users_is_active ON users(is_active);

-- Add comments for clarity
COMMENT ON TABLE users IS 'User hierarchy: MANAGER (root, manager_id=NULL) → LEAD (manager_id set) → MEMBER (lead_id set)';
COMMENT ON COLUMN users.role IS 'User role: MANAGER, LEAD, or MEMBER';
COMMENT ON COLUMN users.manager_id IS 'Reference to the Manager (NULL if user is a Manager)';
COMMENT ON COLUMN users.lead_id IS 'Reference to the Team Lead (NULL if user is Manager or Lead)';
