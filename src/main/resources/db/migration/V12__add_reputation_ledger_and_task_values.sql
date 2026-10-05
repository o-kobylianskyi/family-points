ALTER TABLE task_definitions
    ADD COLUMN IF NOT EXISTS reward_reputation_amount integer,
    ADD COLUMN IF NOT EXISTS penalty_reputation_amount integer;

ALTER TABLE task_instances
    ADD COLUMN IF NOT EXISTS reward_reputation_amount integer,
    ADD COLUMN IF NOT EXISTS penalty_reputation_amount integer;

CREATE TABLE IF NOT EXISTS reputation_transactions (
    id bigserial PRIMARY KEY,
    member_id bigint NOT NULL REFERENCES family_members(id),
    amount integer NOT NULL,
    source_type varchar(40) NOT NULL,
    source_id bigint,
    description varchar(255),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reputation_transactions_member
    ON reputation_transactions(member_id, created_at DESC);
