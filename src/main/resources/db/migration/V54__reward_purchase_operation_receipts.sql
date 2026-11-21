-- V54: Durable, actor-scoped reward purchase idempotency receipts.
-- Parent family lock is held before create/replay; receipts and debit commit atomically.
-- V53 is reserved by the bounded Restore integration candidate.
CREATE TABLE reward_purchase_operation_receipts (
    workspace_id BIGINT NOT NULL REFERENCES families(id) ON DELETE RESTRICT,
    actor_account_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE RESTRICT,
    operation_id UUID NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    request_epoch BIGINT NOT NULL CHECK (request_epoch >= 0),
    route_kind VARCHAR(20) NOT NULL CHECK (route_kind IN ('SELF', 'MANAGER')),
    member_id BIGINT NOT NULL REFERENCES family_members(id) ON DELETE RESTRICT,
    reward_definition_id BIGINT NOT NULL REFERENCES reward_definitions(id) ON DELETE RESTRICT,
    purchase_id BIGINT NOT NULL UNIQUE REFERENCES reward_purchases(id) ON DELETE RESTRICT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (workspace_id, actor_account_id, operation_id)
);
CREATE INDEX idx_reward_purchase_receipts_member
    ON reward_purchase_operation_receipts(workspace_id, member_id, created_at DESC);
