-- Bridge legacy group_permission_grants into the new role catalog permission model.
-- Additive only: legacy grants remain active until authorization is explicitly switched.

CREATE TABLE role_permission_grants (
    id bigserial PRIMARY KEY,
    role_definition_id bigint NOT NULL REFERENCES role_definitions(id),
    permission varchar(40) NOT NULL,
    scope varchar(30) NOT NULL,
    legacy_group_permission_grant_id bigint REFERENCES group_permission_grants(id),
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT role_permission_grants_scope_check CHECK (scope IN ('CURRENT', 'SUBTREE'))
);

CREATE UNIQUE INDEX uk_role_permission_grants_legacy
    ON role_permission_grants(legacy_group_permission_grant_id)
    WHERE legacy_group_permission_grant_id IS NOT NULL;

CREATE UNIQUE INDEX uk_active_role_permission_grant
    ON role_permission_grants(role_definition_id, permission, scope)
    WHERE active = true;

INSERT INTO role_permission_grants (
    role_definition_id,
    permission,
    scope,
    legacy_group_permission_grant_id,
    active
)
SELECT
    rd.id,
    gpg.permission,
    CASE
        WHEN gpg.scope = 'GROUP_SUBTREE' THEN 'SUBTREE'
        ELSE 'CURRENT'
    END,
    gpg.id,
    true
FROM group_permission_grants gpg
JOIN role_definitions rd ON rd.legacy_group_role_id = gpg.group_role_id
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permission_grants rpg
    WHERE rpg.legacy_group_permission_grant_id = gpg.id
);
