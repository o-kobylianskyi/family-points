ALTER TABLE member_groups
    ADD COLUMN group_type varchar(30) NOT NULL DEFAULT 'ORGANIZATIONAL',
    ADD COLUMN valid_from date,
    ADD COLUMN valid_until date;

ALTER TABLE member_groups
    ADD CONSTRAINT member_groups_group_type_check
    CHECK (group_type IN ('ORGANIZATIONAL', 'TEAM'));

ALTER TABLE member_groups
    ADD CONSTRAINT member_groups_valid_period_check
    CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until >= valid_from);

CREATE TABLE group_permission_grants (
    id bigserial PRIMARY KEY,
    member_group_id bigint NOT NULL REFERENCES member_groups(id),
    group_role_id bigint NOT NULL REFERENCES group_roles(id),
    permission varchar(40) NOT NULL,
    scope varchar(30) NOT NULL,
    CONSTRAINT uk_group_permission_grant UNIQUE (member_group_id, group_role_id, permission, scope),
    CONSTRAINT group_permission_grants_scope_check CHECK (scope IN ('GROUP', 'GROUP_SUBTREE'))
);

CREATE INDEX idx_group_permission_grants_role
    ON group_permission_grants(group_role_id);
