-- Hierarchical role catalog foundation.
-- Existing role_sets remain valid and become workspace-shared by default.
ALTER TABLE role_sets
    ADD COLUMN system_code varchar(80),
    ADD COLUMN visibility varchar(20) NOT NULL DEFAULT 'SHARED',
    ADD COLUMN owner_context_type varchar(30) NOT NULL DEFAULT 'WORKSPACE',
    ADD COLUMN owner_context_id bigint,
    ADD COLUMN system_default boolean NOT NULL DEFAULT false;

CREATE UNIQUE INDEX uk_role_set_system_code
    ON role_sets(family_id, system_code)
    WHERE system_code IS NOT NULL;

CREATE TABLE role_definitions (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    role_set_id bigint REFERENCES role_sets(id),
    system_code varchar(80),
    name varchar(120) NOT NULL,
    description varchar(500),
    visibility varchar(20) NOT NULL DEFAULT 'PRIVATE',
    owner_context_type varchar(30) NOT NULL,
    owner_context_id bigint,
    system_default boolean NOT NULL DEFAULT false,
    active boolean NOT NULL DEFAULT true
);

CREATE INDEX idx_role_definitions_workspace ON role_definitions(family_id);
CREATE INDEX idx_role_definitions_set ON role_definitions(role_set_id);
CREATE INDEX idx_role_definitions_owner_context ON role_definitions(family_id, owner_context_type, owner_context_id);
CREATE UNIQUE INDEX uk_role_definition_system_code
    ON role_definitions(family_id, system_code)
    WHERE system_code IS NOT NULL;

CREATE TABLE role_set_bindings (
    id bigserial PRIMARY KEY,
    role_set_id bigint NOT NULL REFERENCES role_sets(id),
    context_type varchar(30) NOT NULL,
    context_id bigint,
    CONSTRAINT uk_role_set_binding UNIQUE NULLS NOT DISTINCT (role_set_id, context_type, context_id)
);

CREATE TABLE role_assignments (
    id bigserial PRIMARY KEY,
    family_id bigint NOT NULL REFERENCES families(id),
    role_definition_id bigint NOT NULL REFERENCES role_definitions(id),
    actor_type varchar(20) NOT NULL,
    actor_id bigint NOT NULL,
    context_type varchar(30) NOT NULL,
    context_id bigint,
    active boolean NOT NULL DEFAULT true
);

CREATE INDEX idx_role_assignments_actor ON role_assignments(family_id, actor_type, actor_id);
CREATE INDEX idx_role_assignments_context ON role_assignments(family_id, context_type, context_id);
CREATE UNIQUE INDEX uk_active_role_assignment
    ON role_assignments(family_id, role_definition_id, actor_type, actor_id, context_type, COALESCE(context_id, -1))
    WHERE active = true;

-- Default reusable role sets. Names are canonical fallbacks; UI translates system_code.
INSERT INTO role_sets (family_id, name, description, active, system_code, visibility, owner_context_type, owner_context_id, system_default)
SELECT f.id, 'Management', 'Default management roles', true, 'MANAGEMENT', 'SHARED', 'WORKSPACE', NULL, true
FROM families f
ON CONFLICT (family_id, name) DO NOTHING;

INSERT INTO role_sets (family_id, name, description, active, system_code, visibility, owner_context_type, owner_context_id, system_default)
SELECT f.id, 'Execution', 'Default execution roles', true, 'EXECUTION', 'SHARED', 'WORKSPACE', NULL, true
FROM families f
ON CONFLICT (family_id, name) DO NOTHING;

INSERT INTO role_sets (family_id, name, description, active, system_code, visibility, owner_context_type, owner_context_id, system_default)
SELECT f.id, 'Control', 'Default control roles', true, 'CONTROL', 'SHARED', 'WORKSPACE', NULL, true
FROM families f
ON CONFLICT (family_id, name) DO NOTHING;

INSERT INTO role_definitions (family_id, role_set_id, system_code, name, visibility, owner_context_type, system_default)
SELECT rs.family_id, rs.id, x.code, x.name, 'SHARED', 'WORKSPACE', true
FROM role_sets rs
JOIN (VALUES
    ('MANAGEMENT','LEADER','Leader'),
    ('MANAGEMENT','DEPUTY','Deputy'),
    ('MANAGEMENT','SENIOR','Senior'),
    ('EXECUTION','EXECUTOR','Executor'),
    ('EXECUTION','ASSISTANT','Assistant'),
    ('CONTROL','REVIEWER','Reviewer'),
    ('CONTROL','OBSERVER','Observer')
) AS x(set_code,code,name) ON x.set_code = rs.system_code
WHERE rs.system_default = true
ON CONFLICT DO NOTHING;
