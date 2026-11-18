-- B-INT-02: additive, fail-closed original creator and RESTORE authorization substrate.
-- Existing families are NOT backfilled: account ordering/admin roles are not
-- evidence of who originally created a family.
CREATE TABLE workspace_original_owner_bindings (
  workspace_id BIGINT PRIMARY KEY REFERENCES families(id) ON DELETE RESTRICT,
  owner_account_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE RESTRICT,
  owner_member_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE RESTRICT,
  provenance VARCHAR(32) NOT NULL CHECK (provenance = 'REGISTRATION_VERIFIED'),
  evidence_id UUID NOT NULL UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
  CONSTRAINT uq_original_owner_workspace_member UNIQUE (workspace_id, owner_member_id)
);

-- Enforce actual account→member→workspace linkage, not only independent FKs.
CREATE FUNCTION verify_original_owner_membership()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM account_workspace_memberships l
    JOIN family_members m ON m.id=l.family_member_id
    WHERE l.account_id=NEW.owner_account_id
      AND l.workspace_id=NEW.workspace_id
      AND l.family_member_id=NEW.owner_member_id
      AND m.family_id=NEW.workspace_id
  ) THEN
    RAISE EXCEPTION 'Original owner membership mismatch' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER trg_original_owner_membership
BEFORE INSERT OR UPDATE ON workspace_original_owner_bindings
FOR EACH ROW EXECUTE FUNCTION verify_original_owner_membership();

CREATE TABLE workspace_restore_permits (
  operation_id UUID PRIMARY KEY,
  target_workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
  owner_account_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE RESTRICT,
  owner_member_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE RESTRICT,
  scope_digest CHAR(64) NOT NULL CHECK (scope_digest ~ '^[0-9a-f]{64}$'),
  credential_fingerprint CHAR(64) NOT NULL CHECK (credential_fingerprint ~ '^[0-9a-f]{64}$'),
  target_expected_epoch BIGINT NOT NULL CHECK (target_expected_epoch >= 0),
  issued_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  consumed_at TIMESTAMPTZ,
  CONSTRAINT ck_restore_permit_ttl CHECK (expires_at > issued_at
      AND expires_at <= issued_at + interval '5 minutes'),
  CONSTRAINT ck_restore_permit_consumed CHECK (consumed_at IS NULL
      OR consumed_at >= issued_at),
  CONSTRAINT uq_restore_permit_target_operation UNIQUE (target_workspace_id,operation_id)
);
CREATE INDEX idx_restore_permits_target ON workspace_restore_permits(target_workspace_id,issued_at);

CREATE TABLE workspace_restore_receipts (
  operation_id UUID PRIMARY KEY REFERENCES workspace_restore_permits(operation_id) ON DELETE RESTRICT,
  target_workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
  scope_digest CHAR(64) NOT NULL CHECK (scope_digest ~ '^[0-9a-f]{64}$'),
  manifest_root_digest CHAR(64) NOT NULL CHECK (manifest_root_digest ~ '^[0-9a-f]{64}$'),
  target_epoch_before BIGINT NOT NULL CHECK (target_epoch_before >= 0),
  target_epoch_after BIGINT NOT NULL CHECK (target_epoch_after = target_epoch_before + 1),
  category_counts JSONB NOT NULL,
  receipt_digest CHAR(64) NOT NULL CHECK (receipt_digest ~ '^[0-9a-f]{64}$'),
  completed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);
-- Immutable audit ledger. No ON DELETE CASCADE and no mutable receipts.
CREATE FUNCTION deny_restore_receipt_mutation()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'RESTORE receipt immutable' USING ERRCODE='23514';
END $$;
CREATE TRIGGER trg_restore_receipt_immutable
BEFORE UPDATE OR DELETE ON workspace_restore_receipts
FOR EACH ROW EXECUTE FUNCTION deny_restore_receipt_mutation();

-- Separate persistent RESTORE credential budget, never reset the RESET limits.
CREATE TABLE workspace_restore_reauth_limits (
  workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
  account_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE RESTRICT,
  window_started_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
  failed_attempts INT NOT NULL DEFAULT 0 CHECK (failed_attempts BETWEEN 0 AND 10),
  PRIMARY KEY (workspace_id,account_id)
);
-- No controller/HTTP executable route is created here. Production remains HOLD.
