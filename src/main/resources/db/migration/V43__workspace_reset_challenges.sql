-- BKL-002: expiring, scoped challenge records; does not enable reset execution.
CREATE TABLE workspace_reset_challenges (
  id UUID PRIMARY KEY,
  workspace_id BIGINT NOT NULL,
  operation_id UUID NOT NULL,
  account_id BIGINT NOT NULL,
  verifier_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reset_challenge_operation FOREIGN KEY (workspace_id, operation_id)
    REFERENCES workspace_reset_operations(workspace_id, operation_id) ON DELETE RESTRICT,
  CONSTRAINT uq_reset_challenge_operation UNIQUE(workspace_id, operation_id),
  CONSTRAINT ck_reset_challenge_verifier CHECK (verifier_hash ~ '^[0-9a-f]{64}$')
);
CREATE INDEX idx_reset_challenge_expiry ON workspace_reset_challenges(expires_at);
