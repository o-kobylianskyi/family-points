-- Limits apply per member and, independently, across the entire workspace.
-- NULL means unlimited. All existing rewards remain unlimited by default.
ALTER TABLE reward_definitions
    ADD COLUMN member_total_limit INTEGER,
    ADD COLUMN member_daily_limit INTEGER,
    ADD COLUMN member_weekly_limit INTEGER,
    ADD COLUMN workspace_total_limit INTEGER,
    ADD COLUMN workspace_daily_limit INTEGER,
    ADD COLUMN workspace_weekly_limit INTEGER,
    ADD COLUMN extra_request_allowed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_member_total_limit CHECK (member_total_limit IS NULL OR member_total_limit > 0);
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_member_daily_limit CHECK (member_daily_limit IS NULL OR member_daily_limit > 0);
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_member_weekly_limit CHECK (member_weekly_limit IS NULL OR member_weekly_limit > 0);
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_workspace_total_limit CHECK (workspace_total_limit IS NULL OR workspace_total_limit > 0);
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_workspace_daily_limit CHECK (workspace_daily_limit IS NULL OR workspace_daily_limit > 0);
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_workspace_weekly_limit CHECK (workspace_weekly_limit IS NULL OR workspace_weekly_limit > 0);
CREATE INDEX idx_reward_purchase_limit_lookup ON reward_purchases(reward_definition_id, purchased_at);
