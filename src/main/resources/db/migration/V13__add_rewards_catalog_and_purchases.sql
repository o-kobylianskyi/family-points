CREATE TABLE IF NOT EXISTS reward_definitions (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    title varchar(150) NOT NULL,
    description varchar(1000),
    point_type_id bigint NOT NULL REFERENCES point_types(id),
    price_amount integer NOT NULL,
    minimum_reputation integer,
    requires_approval boolean NOT NULL DEFAULT false,
    active boolean NOT NULL DEFAULT true,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reward_definitions_family_active
    ON reward_definitions(family_id, active);

CREATE TABLE IF NOT EXISTS reward_purchases (
    id bigserial PRIMARY KEY,
    reward_definition_id bigint NOT NULL REFERENCES reward_definitions(id),
    member_id bigint NOT NULL REFERENCES family_members(id),
    reward_title varchar(150) NOT NULL,
    point_type_id bigint NOT NULL REFERENCES point_types(id),
    price_amount integer NOT NULL,
    status varchar(30) NOT NULL,
    purchased_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_reward_purchases_member
    ON reward_purchases(member_id, purchased_at DESC);

CREATE INDEX IF NOT EXISTS idx_reward_purchases_definition
    ON reward_purchases(reward_definition_id, purchased_at DESC);
