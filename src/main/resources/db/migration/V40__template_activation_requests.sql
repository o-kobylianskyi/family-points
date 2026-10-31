-- Transaction-scoped deduplication for explicit template activation.
-- The insert precedes object creation within the same transaction, so failures roll back the claim.
CREATE TABLE template_activation_requests (
    workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    operation_id UUID NOT NULL,
    template_id BIGINT NOT NULL REFERENCES workspace_private_templates(id) ON DELETE CASCADE,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('TASK','REWARD')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, operation_id)
);
CREATE INDEX idx_template_activation_template ON template_activation_requests(workspace_id,template_id);
