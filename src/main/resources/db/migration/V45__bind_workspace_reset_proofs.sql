-- BKL-002 internal proof binding. This does NOT enable reset execution.
-- Existing V43 rows intentionally retain NULL bindings and cannot be consumed.
ALTER TABLE workspace_reset_challenges
    ADD COLUMN data_epoch BIGINT,
    ADD COLUMN snapshot_digest CHAR(64),
    ADD COLUMN requested_by_member_id BIGINT,
    ADD COLUMN credential_fingerprint CHAR(64),
    ADD CONSTRAINT ck_reset_proof_binding CHECK (
        (data_epoch IS NULL AND snapshot_digest IS NULL AND requested_by_member_id IS NULL AND credential_fingerprint IS NULL)
        OR
        (data_epoch IS NOT NULL AND data_epoch >= 0
         AND snapshot_digest IS NOT NULL AND snapshot_digest ~ '^[0-9a-f]{64}$'
         AND requested_by_member_id IS NOT NULL AND requested_by_member_id > 0
         AND credential_fingerprint IS NOT NULL AND credential_fingerprint ~ '^[0-9a-f]{64}$')
    );
-- Actor IDs are immutable audit values, not new cascading FKs to protected identities.
