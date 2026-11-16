-- BKL-002: durable evidence for server-local encrypted artifact staging.
-- Physical bytes + SHA-256 are NOT proof that a family can be restored.
-- Never set restore_verified_at or VERIFIED from these fields alone.
ALTER TABLE workspace_protective_backups
    ADD COLUMN artifact_sha256 CHAR(64),
    ADD COLUMN artifact_bytes BIGINT,
    ADD COLUMN artifact_stored_at TIMESTAMPTZ;

ALTER TABLE workspace_protective_backups
    ADD CONSTRAINT ck_protective_backup_artifact_evidence_complete
    CHECK (
        (artifact_sha256 IS NULL AND artifact_bytes IS NULL AND artifact_stored_at IS NULL)
        OR
        (artifact_sha256 ~ '^[0-9a-f]{64}$' AND artifact_bytes > 0
            AND artifact_bytes <= 67140864 AND artifact_stored_at IS NOT NULL
            AND encrypted = TRUE AND storage_key IS NOT NULL)
    );

CREATE INDEX idx_protective_backup_staged_expiry
    ON workspace_protective_backups(expires_at, workspace_id)
    WHERE artifact_stored_at IS NOT NULL
      AND state NOT IN ('EXPIRED','RESET_COMPLETED');
