-- Issue #18: replayable, workspace-scoped import receipts for explicitly confirmed catalog selections.
-- Additive only. Does not backfill, import templates, activate definitions or mutate ledgers.
CREATE TABLE workspace_template_import_receipts (
    workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
    operation_id UUID NOT NULL,
    request_sha256 CHAR(64) NOT NULL,
    selected_codes_json TEXT NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (workspace_id, operation_id),
    CONSTRAINT workspace_template_import_receipt_sha CHECK (request_sha256 ~ '^[a-f0-9]{64}$')
);
CREATE INDEX idx_workspace_template_import_receipts_created
    ON workspace_template_import_receipts(workspace_id,created_at);
