-- Complete the transition from legacy GroupRole copies to reusable RoleDefinition + RoleAssignment.
-- Existing legacy tables remain for compatibility but are no longer the source of truth for new group-role usage.

CREATE TABLE IF NOT EXISTS task_definition_required_role_definitions (
    task_definition_id bigint NOT NULL REFERENCES task_definitions(id) ON DELETE CASCADE,
    role_definition_id bigint NOT NULL REFERENCES role_definitions(id),
    PRIMARY KEY (task_definition_id, role_definition_id)
);

INSERT INTO task_definition_required_role_definitions (task_definition_id, role_definition_id)
SELECT legacy.task_definition_id, rd.id
FROM task_definition_required_group_roles legacy
JOIN role_definitions rd ON rd.legacy_group_role_id = legacy.group_role_id
ON CONFLICT DO NOTHING;

-- Default permission presets for reusable system roles.
-- These are contextual permissions; RoleAssignment decides in which group an actor receives the role.
WITH presets(system_code, permission, scope) AS (
    VALUES
        ('LEADER','GROUP_VIEW','CURRENT'),
        ('LEADER','GROUP_MANAGE','CURRENT'),
        ('LEADER','MEMBER_VIEW','CURRENT'),
        ('LEADER','MEMBER_MANAGE','CURRENT'),
        ('LEADER','TASK_VIEW','CURRENT'),
        ('LEADER','TASK_CREATE','CURRENT'),
        ('LEADER','TASK_ASSIGN','CURRENT'),
        ('LEADER','TASK_MANAGE','CURRENT'),
        ('LEADER','TASK_APPROVE','CURRENT'),
        ('LEADER','POINT_VIEW','CURRENT'),
        ('LEADER','POINT_AWARD','CURRENT'),
        ('LEADER','SUBGROUP_MANAGE','CURRENT'),

        ('DEPUTY','GROUP_VIEW','CURRENT'),
        ('DEPUTY','GROUP_MANAGE','CURRENT'),
        ('DEPUTY','MEMBER_VIEW','CURRENT'),
        ('DEPUTY','MEMBER_MANAGE','CURRENT'),
        ('DEPUTY','TASK_VIEW','CURRENT'),
        ('DEPUTY','TASK_CREATE','CURRENT'),
        ('DEPUTY','TASK_ASSIGN','CURRENT'),
        ('DEPUTY','TASK_MANAGE','CURRENT'),
        ('DEPUTY','TASK_APPROVE','CURRENT'),
        ('DEPUTY','POINT_VIEW','CURRENT'),
        ('DEPUTY','POINT_AWARD','CURRENT'),
        ('DEPUTY','SUBGROUP_MANAGE','CURRENT'),

        ('SENIOR','GROUP_VIEW','CURRENT'),
        ('SENIOR','MEMBER_VIEW','CURRENT'),
        ('SENIOR','TASK_VIEW','CURRENT'),
        ('SENIOR','TASK_ASSIGN','CURRENT'),
        ('SENIOR','TASK_APPROVE','CURRENT'),

        ('EXECUTOR','TASK_VIEW','CURRENT'),
        ('ASSISTANT','TASK_VIEW','CURRENT'),

        ('REVIEWER','GROUP_VIEW','CURRENT'),
        ('REVIEWER','MEMBER_VIEW','CURRENT'),
        ('REVIEWER','TASK_VIEW','CURRENT'),
        ('REVIEWER','TASK_APPROVE','CURRENT'),
        ('REVIEWER','POINT_VIEW','CURRENT'),

        ('OBSERVER','GROUP_VIEW','CURRENT'),
        ('OBSERVER','MEMBER_VIEW','CURRENT'),
        ('OBSERVER','TASK_VIEW','CURRENT'),
        ('OBSERVER','POINT_VIEW','CURRENT')
)
INSERT INTO role_permission_grants (role_definition_id, permission, scope, active)
SELECT rd.id, p.permission, p.scope, true
FROM presets p
JOIN role_definitions rd ON rd.system_code = p.system_code
WHERE rd.system_default = true
  AND NOT EXISTS (
      SELECT 1
      FROM role_permission_grants existing
      WHERE existing.role_definition_id = rd.id
        AND existing.permission = p.permission
        AND existing.scope = p.scope
        AND existing.active = true
  );
