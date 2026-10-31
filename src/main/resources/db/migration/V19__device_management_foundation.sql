-- FamilyPoints device management foundation.
-- A device has one permanent owner; a temporary child session does not reassign ownership.
CREATE TABLE managed_devices (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES families(id),
    owner_member_id BIGINT NOT NULL REFERENCES family_members(id),
    display_name VARCHAR(120) NOT NULL,
    installation_id UUID NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    management_mode VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    device_role VARCHAR(30) NOT NULL DEFAULT 'PARENT_PERSONAL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_device_platform CHECK (platform IN ('ANDROID', 'WINDOWS')),
    CONSTRAINT ck_device_mode CHECK (management_mode IN ('STANDARD', 'DEVICE_OWNER')),
    CONSTRAINT ck_device_role CHECK (device_role IN ('CHILD_PERSONAL', 'PARENT_PERSONAL', 'SHARED'))
);
CREATE INDEX ix_managed_devices_workspace ON managed_devices(workspace_id);
CREATE INDEX ix_managed_devices_owner ON managed_devices(owner_member_id);

CREATE TABLE device_installed_apps (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES managed_devices(id) ON DELETE CASCADE,
    package_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    system_app BOOLEAN NOT NULL DEFAULT FALSE,
    installed BOOLEAN NOT NULL DEFAULT TRUE,
    last_scanned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_device_package UNIQUE(device_id, package_name)
);

-- Policies specify intent independently of device capability.
-- Actual enforcement status will be reported by Android separately.
CREATE TABLE device_app_policies (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES managed_devices(id) ON DELETE CASCADE,
    package_name VARCHAR(255) NOT NULL,
    access_mode VARCHAR(30) NOT NULL DEFAULT 'UNRESTRICTED',
    admin_visible BOOLEAN NOT NULL DEFAULT TRUE,
    child_visible BOOLEAN NOT NULL DEFAULT TRUE,
    daily_limit_minutes INTEGER,
    session_limit_minutes INTEGER,
    minute_price INTEGER,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_device_app_policy UNIQUE(device_id, package_name),
    CONSTRAINT ck_app_access CHECK(access_mode IN ('UNRESTRICTED', 'POINTS', 'SCHEDULE', 'POINTS_AND_SCHEDULE', 'PARENT_APPROVAL', 'BLOCKED')),
    CONSTRAINT ck_daily_limit CHECK(daily_limit_minutes IS NULL OR daily_limit_minutes > 0),
    CONSTRAINT ck_session_limit CHECK(session_limit_minutes IS NULL OR session_limit_minutes > 0),
    CONSTRAINT ck_minute_price CHECK(minute_price IS NULL OR minute_price >= 0)
);

CREATE TABLE temporary_child_sessions (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES managed_devices(id),
    child_member_id BIGINT NOT NULL REFERENCES family_members(id),
    started_by_member_id BIGINT NOT NULL REFERENCES family_members(id),
    ended_by_member_id BIGINT REFERENCES family_members(id),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    charge_mode VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT ck_child_session_charge CHECK (charge_mode IN ('STANDARD', 'FREE')),
    CONSTRAINT ck_child_session_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_child_session_expiry CHECK (expires_at IS NULL OR expires_at > started_at),
    CONSTRAINT ck_child_session_end CHECK (ended_at IS NULL OR ended_at >= started_at)
);
CREATE UNIQUE INDEX uq_device_active_child_session
    ON temporary_child_sessions(device_id) WHERE status = 'ACTIVE';
CREATE INDEX ix_temporary_child_sessions_child ON temporary_child_sessions(child_member_id, started_at DESC);

-- A parent override unlocks FamilyPoints policies only, not OS security.
CREATE TABLE device_parent_override_sessions (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES managed_devices(id),
    authorized_by_member_id BIGINT NOT NULL REFERENCES family_members(id),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_override_expiry CHECK(expires_at > started_at)
);
