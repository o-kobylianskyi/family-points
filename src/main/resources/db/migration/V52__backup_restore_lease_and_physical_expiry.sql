-- BKL-002: tenant-scoped in-use leases for encrypted local snapshots.
-- Does not enable an automated destructive cleanup job.
CREATE TABLE workspace_backup_restore_leases (
    lease_id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL REFERENCES workspace_protective_backups(snapshot_id) ON DELETE RESTRICT,
    workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_backup_lease_expiration CHECK (expires_at > created_at)
);
CREATE INDEX idx_backup_restore_lease_snapshot_expiry
    ON workspace_backup_restore_leases(snapshot_id,expires_at);
CREATE INDEX idx_backup_restore_lease_expiry
    ON workspace_backup_restore_leases(expires_at);
