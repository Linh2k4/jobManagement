--liquibase formatted sql

--changeset jobmanagement:V22

-- Profile screen (Cài đặt → Hồ sơ) collects these fields but the User
-- entity had nowhere to put them — the form only console.log'd instead of
-- saving anything.

ALTER TABLE users ADD COLUMN phone VARCHAR(30);
ALTER TABLE users ADD COLUMN birth_date DATE;
ALTER TABLE users ADD COLUMN address TEXT;
ALTER TABLE users ADD COLUMN bio TEXT;
