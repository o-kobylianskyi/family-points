CREATE TABLE task_audit_events (
    id BIGSERIAL PRIMARY KEY,
    family_id BIGINT NOT NULL,
    task_definition_id BIGINT NOT NULL,
    task_instance_id BIGINT NULL,
    event_type VARCHAR(40) NOT NULL,
    performed_by_member_id BIGINT NULL,
    occurred_at TIMESTAMP NOT NULL,
    from_actor_type VARCHAR(20) NULL,
    from_actor_id BIGINT NULL,
    to_actor_type VARCHAR(20) NULL,
    to_actor_id BIGINT NULL,
    details VARCHAR(1000) NULL,

    CONSTRAINT fk_task_audit_workspace
        FOREIGN KEY (family_id) REFERENCES families(id),

    CONSTRAINT fk_task_audit_definition
        FOREIGN KEY (task_definition_id) REFERENCES task_definitions(id),

    CONSTRAINT fk_task_audit_instance
        FOREIGN KEY (task_instance_id) REFERENCES task_instances(id),

    CONSTRAINT fk_task_audit_performed_by
        FOREIGN KEY (performed_by_member_id) REFERENCES family_members(id),

    CONSTRAINT ck_task_audit_from_actor
        CHECK (
            (from_actor_type IS NULL AND from_actor_id IS NULL)
            OR
            (from_actor_type IN ('MEMBER', 'GROUP') AND from_actor_id IS NOT NULL)
        ),

    CONSTRAINT ck_task_audit_to_actor
        CHECK (
            (to_actor_type IS NULL AND to_actor_id IS NULL)
            OR
            (to_actor_type IN ('MEMBER', 'GROUP') AND to_actor_id IS NOT NULL)
        )
);

CREATE INDEX idx_task_audit_definition_time
    ON task_audit_events(task_definition_id, occurred_at);

CREATE INDEX idx_task_audit_instance_time
    ON task_audit_events(task_instance_id, occurred_at);

CREATE INDEX idx_task_audit_workspace_time
    ON task_audit_events(family_id, occurred_at);
