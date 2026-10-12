-- Optional cooldown for repeat purchases by the same member.
-- NULL = no cooldown; defaults to 30 seconds for new direct rewards.
ALTER TABLE reward_definitions ADD COLUMN cooldown_seconds INTEGER NOT NULL DEFAULT 30;
ALTER TABLE reward_definitions ADD CONSTRAINT chk_reward_cooldown_nonnegative CHECK (cooldown_seconds >= 0);
CREATE INDEX idx_reward_purchases_cooldown ON reward_purchases (member_id, reward_definition_id, purchased_at DESC);
