ALTER TABLE reward_requirements
    ADD COLUMN IF NOT EXISTS blocking_mode varchar(30) NOT NULL DEFAULT 'NONE';

CREATE TABLE IF NOT EXISTS reward_requirement_blocked_categories (
    requirement_id bigint NOT NULL REFERENCES reward_requirements(id) ON DELETE CASCADE,
    category_id bigint NOT NULL REFERENCES reward_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (requirement_id, category_id)
);

CREATE TABLE IF NOT EXISTS reward_requirement_blocked_rewards (
    requirement_id bigint NOT NULL REFERENCES reward_requirements(id) ON DELETE CASCADE,
    reward_definition_id bigint NOT NULL REFERENCES reward_definitions(id) ON DELETE CASCADE,
    PRIMARY KEY (requirement_id, reward_definition_id)
);
