-- Grant shared role-catalog administration to the predefined PARENT workspace role.
-- Existing workspaces need this because DefaultWorkspaceRoles only affects newly created roles.

INSERT INTO family_role_permissions (role_id, permission)
SELECT fr.id, 'MANAGE_ROLES'
FROM family_roles fr
WHERE fr.code = 'PARENT'
  AND NOT EXISTS (
      SELECT 1
      FROM family_role_permissions p
      WHERE p.role_id = fr.id
        AND p.permission = 'MANAGE_ROLES'
  );
