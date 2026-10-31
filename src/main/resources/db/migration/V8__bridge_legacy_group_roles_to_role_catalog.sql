-- Bridge legacy group_roles/group_membership_roles into the hierarchical role catalog.
-- This migration is intentionally additive. Legacy tables remain the active compatibility path
-- until services/API/frontend are switched and smoke-tested.

ALTER TABLE role_definitions
    ADD COLUMN legacy_group_role_id bigint REFERENCES group_roles(id);

CREATE UNIQUE INDEX uk_role_definitions_legacy_group_role
    ON role_definitions(legacy_group_role_id)
    WHERE legacy_group_role_id IS NOT NULL;

-- Every legacy GroupRole is group-local by definition, therefore migrate it as a PRIVATE
-- RoleDefinition owned by that MEMBER_GROUP. Existing RoleSet association is preserved.
INSERT INTO role_definitions (
    family_id,
    role_set_id,
    system_code,
    name,
    description,
    visibility,
    owner_context_type,
    owner_context_id,
    system_default,
    active,
    legacy_group_role_id
)
SELECT
    mg.family_id,
    gr.role_set_id,
    NULL,
    gr.name,
    gr.description,
    'PRIVATE',
    'MEMBER_GROUP',
    gr.member_group_id,
    false,
    true,
    gr.id
FROM group_roles gr
JOIN member_groups mg ON mg.id = gr.member_group_id
WHERE NOT EXISTS (
    SELECT 1
    FROM role_definitions rd
    WHERE rd.legacy_group_role_id = gr.id
);

-- Preserve member -> group-role assignments as contextual RoleAssignments.
-- Inactive memberships are copied as inactive assignments for historical continuity.
INSERT INTO role_assignments (
    family_id,
    role_definition_id,
    actor_type,
    actor_id,
    context_type,
    context_id,
    active
)
SELECT
    mgp.family_id,
    rd.id,
    'MEMBER',
    gm.member_id,
    'MEMBER_GROUP',
    gm.member_group_id,
    gm.active
FROM group_membership_roles gmr
JOIN group_memberships gm ON gm.id = gmr.membership_id
JOIN member_groups mgp ON mgp.id = gm.member_group_id
JOIN role_definitions rd ON rd.legacy_group_role_id = gmr.group_role_id
WHERE NOT EXISTS (
    SELECT 1
    FROM role_assignments ra
    WHERE ra.family_id = mgp.family_id
      AND ra.role_definition_id = rd.id
      AND ra.actor_type = 'MEMBER'
      AND ra.actor_id = gm.member_id
      AND ra.context_type = 'MEMBER_GROUP'
      AND ra.context_id = gm.member_group_id
      AND ra.active = gm.active
);
