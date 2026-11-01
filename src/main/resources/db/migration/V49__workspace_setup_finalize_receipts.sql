-- Issue18: immutable receipts for explicit, atomic private-template import + wizard closure.
-- No automatic tasks, rewards, groups, purchases, ledger mutations or legacy backfill.
CREATE TABLE workspace_setup_finalize_receipts (
 workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
 operation_id UUID NOT NULL,
 data_epoch BIGINT NOT NULL CHECK (data_epoch>=0),
 expected_revision BIGINT NOT NULL CHECK (expected_revision>=0),
 request_sha256 CHAR(64) NOT NULL CHECK (request_sha256 ~ '^[a-f0-9]{64}$'),
 result_json TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY (workspace_id,operation_id)
);
CREATE INDEX idx_workspace_setup_finalize_receipts_created
 ON workspace_setup_finalize_receipts(workspace_id,created_at);
