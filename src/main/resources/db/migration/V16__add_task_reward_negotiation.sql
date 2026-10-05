CREATE TABLE IF NOT EXISTS task_reward_requests (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    task_instance_id bigint NOT NULL REFERENCES task_instances(id) ON DELETE CASCADE,
    requested_by_member_id bigint NOT NULL REFERENCES family_members(id),
    status varchar(30) NOT NULL,

    requested_point_type_id bigint REFERENCES point_types(id),
    requested_point_amount integer,
    requested_reputation_amount integer,
    requested_reward_definition_id bigint REFERENCES reward_definitions(id),
    requested_custom_reward_title varchar(150),
    requested_comment varchar(500),

    approved_point_type_id bigint REFERENCES point_types(id),
    approved_point_amount integer,
    approved_reputation_amount integer,
    approved_reward_definition_id bigint REFERENCES reward_definitions(id),
    approved_custom_reward_title varchar(150),
    reviewer_comment varchar(500),

    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at timestamp,
    fulfilled_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_task_reward_requests_instance
    ON task_reward_requests(task_instance_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_task_reward_requests_family_status
    ON task_reward_requests(family_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_task_reward_request_active_instance_member
    ON task_reward_requests(task_instance_id, requested_by_member_id)
    WHERE status IN ('REQUESTED', 'APPROVED');
