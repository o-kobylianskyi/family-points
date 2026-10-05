ALTER TABLE reward_definitions
    ADD COLUMN IF NOT EXISTS acquisition_mode varchar(30) NOT NULL DEFAULT 'DIRECT';

CREATE TABLE IF NOT EXISTS reward_categories (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    name varchar(100) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    CONSTRAINT uk_reward_category_family_name UNIQUE (family_id, name)
);

CREATE TABLE IF NOT EXISTS reward_definition_categories (
    reward_definition_id bigint NOT NULL REFERENCES reward_definitions(id) ON DELETE CASCADE,
    category_id bigint NOT NULL REFERENCES reward_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (reward_definition_id, category_id)
);

CREATE TABLE IF NOT EXISTS reward_requests (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    requested_by_member_id bigint NOT NULL REFERENCES family_members(id),
    reward_definition_id bigint REFERENCES reward_definitions(id),
    title varchar(150) NOT NULL,
    description varchar(1000),
    status varchar(40) NOT NULL,
    approved_point_type_id bigint REFERENCES point_types(id),
    approved_price_amount integer,
    minimum_reputation integer,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at timestamp,
    completed_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_reward_requests_family_status
    ON reward_requests(family_id, status);

CREATE INDEX IF NOT EXISTS idx_reward_requests_member_created
    ON reward_requests(requested_by_member_id, created_at DESC);

CREATE TABLE IF NOT EXISTS reward_requirements (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    reward_definition_id bigint REFERENCES reward_definitions(id) ON DELETE CASCADE,
    reward_request_id bigint REFERENCES reward_requests(id) ON DELETE CASCADE,
    phase varchar(30) NOT NULL,
    requirement_type varchar(40) NOT NULL,
    task_definition_id bigint REFERENCES task_definitions(id),
    time_scope varchar(30),
    window_value integer,
    description varchar(255),
    required boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    CONSTRAINT ck_reward_requirement_owner CHECK (
        (reward_definition_id IS NOT NULL AND reward_request_id IS NULL)
        OR (reward_definition_id IS NULL AND reward_request_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_reward_requirements_definition
    ON reward_requirements(reward_definition_id, phase);

CREATE INDEX IF NOT EXISTS idx_reward_requirements_request
    ON reward_requirements(reward_request_id, phase);

ALTER TABLE reward_purchases
    ALTER COLUMN reward_definition_id DROP NOT NULL;

ALTER TABLE reward_purchases
    ADD COLUMN IF NOT EXISTS reward_request_id bigint REFERENCES reward_requests(id);

CREATE TABLE IF NOT EXISTS reward_obligations (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    member_id bigint NOT NULL REFERENCES family_members(id),
    reward_purchase_id bigint NOT NULL REFERENCES reward_purchases(id) ON DELETE CASCADE,
    requirement_id bigint REFERENCES reward_requirements(id),
    title varchar(255) NOT NULL,
    status varchar(30) NOT NULL,
    blocking_mode varchar(30) NOT NULL DEFAULT 'NONE',
    due_at timestamp,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_reward_obligations_member_status
    ON reward_obligations(member_id, status);

CREATE TABLE IF NOT EXISTS reward_obligation_blocked_categories (
    obligation_id bigint NOT NULL REFERENCES reward_obligations(id) ON DELETE CASCADE,
    category_id bigint NOT NULL REFERENCES reward_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (obligation_id, category_id)
);

CREATE TABLE IF NOT EXISTS reward_obligation_blocked_rewards (
    obligation_id bigint NOT NULL REFERENCES reward_obligations(id) ON DELETE CASCADE,
    reward_definition_id bigint NOT NULL REFERENCES reward_definitions(id) ON DELETE CASCADE,
    PRIMARY KEY (obligation_id, reward_definition_id)
);

CREATE INDEX IF NOT EXISTS idx_reward_definition_categories_category
    ON reward_definition_categories(category_id);
