-- Additive immutable result references for explicitly confirmed TASK/REWARD activation.
-- No backfill and no activation or modification of existing family data.
CREATE TABLE template_activation_receipts (
  workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
  operation_id UUID NOT NULL,
  template_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL CHECK (kind IN ('TASK','REWARD')),
  request_fingerprint CHAR(64) NOT NULL CHECK (request_fingerprint ~ '^[a-f0-9]{64}$'),
  created_definition_id BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, operation_id)
);
CREATE INDEX idx_template_activation_receipts_template ON template_activation_receipts(workspace_id,template_id);
