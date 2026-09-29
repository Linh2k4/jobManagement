--liquibase formatted sql

--changeset jobmanagement:V8

-- Create category management tables (Phase 3)
-- Implements Scope.md §2.4 Task categorization with custom fields

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon VARCHAR(50),
    color VARCHAR(7),
    tier VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    owner_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    owner_name VARCHAR(255),
    allowed_task_types JSONB DEFAULT '[]',
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    task_count BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 1,
    CONSTRAINT category_tier_check CHECK (tier IN ('SYSTEM', 'CUSTOM')),
    CONSTRAINT category_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_categories_tier ON categories(tier);
CREATE INDEX idx_categories_owner_id ON categories(owner_id);
CREATE INDEX idx_categories_status ON categories(status);
CREATE INDEX idx_categories_code ON categories(code);

COMMENT ON TABLE categories IS 'Task categories: SYSTEM (Manager) or CUSTOM (Lead) (Scope §2.4)';
COMMENT ON COLUMN categories.tier IS 'SYSTEM=company-wide, CUSTOM=lead-owned (max 20 per lead)';
COMMENT ON COLUMN categories.code IS 'Unique identifier for category (generated from name)';
COMMENT ON COLUMN categories.allowed_task_types IS 'JSON array of allowed task type IDs';

CREATE TABLE extra_fields (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    required BOOLEAN DEFAULT false,
    placeholder VARCHAR(500),
    default_value TEXT,
    options JSONB DEFAULT '[]',
    sort_order INTEGER DEFAULT 0,
    visible_in_list BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    version BIGINT DEFAULT 1,
    CONSTRAINT field_type_check CHECK (field_type IN ('TEXT', 'TEXTAREA', 'NUMBER', 'DATE', 'DATETIME', 'SELECT', 'MULTISELECT', 'CHECKBOX'))
);

CREATE INDEX idx_extra_fields_category_id ON extra_fields(category_id);
CREATE INDEX idx_extra_fields_sort ON extra_fields(category_id, sort_order);

COMMENT ON TABLE extra_fields IS 'Custom fields per category (Scope §2.4 category definition)';
COMMENT ON COLUMN extra_fields.field_type IS '8 types: TEXT, TEXTAREA, NUMBER, DATE, DATETIME, SELECT, MULTISELECT, CHECKBOX';
COMMENT ON COLUMN extra_fields.options IS 'JSON array for SELECT/MULTISELECT types';
COMMENT ON COLUMN extra_fields.visible_in_list IS 'Whether field appears in task list view';
