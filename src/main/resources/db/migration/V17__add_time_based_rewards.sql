ALTER TABLE reward_definitions
    ADD COLUMN IF NOT EXISTS reward_kind varchar(30) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN IF NOT EXISTS default_duration_minutes integer;

ALTER TABLE reward_requests
    ADD COLUMN IF NOT EXISTS approved_duration_minutes integer;

ALTER TABLE reward_purchases
    ADD COLUMN IF NOT EXISTS duration_minutes integer;

ALTER TABLE task_reward_requests
    ADD COLUMN IF NOT EXISTS requested_duration_minutes integer,
    ADD COLUMN IF NOT EXISTS approved_duration_minutes integer;

ALTER TABLE reward_definitions
    ADD CONSTRAINT ck_reward_default_duration_positive
    CHECK (default_duration_minutes IS NULL OR default_duration_minutes > 0);

ALTER TABLE reward_requests
    ADD CONSTRAINT ck_reward_request_duration_positive
    CHECK (approved_duration_minutes IS NULL OR approved_duration_minutes > 0);

ALTER TABLE reward_purchases
    ADD CONSTRAINT ck_reward_purchase_duration_positive
    CHECK (duration_minutes IS NULL OR duration_minutes > 0);

ALTER TABLE task_reward_requests
    ADD CONSTRAINT ck_task_reward_requested_duration_positive
    CHECK (requested_duration_minutes IS NULL OR requested_duration_minutes > 0);

ALTER TABLE task_reward_requests
    ADD CONSTRAINT ck_task_reward_approved_duration_positive
    CHECK (approved_duration_minutes IS NULL OR approved_duration_minutes > 0);
