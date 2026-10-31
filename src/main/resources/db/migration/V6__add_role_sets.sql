CREATE TABLE role_sets (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    name varchar(120) NOT NULL,
    description varchar(500),
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_role_set_name UNIQUE (family_id, name)
);

ALTER TABLE group_roles
    ADD COLUMN role_set_id bigint REFERENCES role_sets(id);

CREATE INDEX idx_group_roles_role_set ON group_roles(role_set_id);
