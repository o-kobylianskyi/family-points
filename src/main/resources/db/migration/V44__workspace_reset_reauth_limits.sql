-- Persistent failed password reauthentication budget, scoped per account and family.
-- No family data is deleted or reset.
CREATE TABLE workspace_reset_reauth_limits (
  workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
  account_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE RESTRICT,
  window_started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts BETWEEN 0 AND 1000),
  PRIMARY KEY (workspace_id, account_id)
);
