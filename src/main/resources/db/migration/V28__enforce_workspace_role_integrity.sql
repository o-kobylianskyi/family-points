-- Guard tenant boundaries even when writes bypass application services.
-- Existing inconsistent rows deliberately fail migration; never silently reassign users.
CREATE OR REPLACE FUNCTION enforce_workspace_member_role()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM family_roles r
        WHERE r.id = NEW.family_role_id AND r.family_id = NEW.family_id
    ) THEN
        RAISE EXCEPTION 'Member role belongs to another workspace'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM family_members m
        LEFT JOIN family_roles r ON r.id = m.family_role_id AND r.family_id = m.family_id
        WHERE r.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Existing member role/workspace mismatch';
    END IF;
END;
$$;

CREATE TRIGGER trg_workspace_member_role
BEFORE INSERT OR UPDATE OF family_role_id, family_id
ON family_members
FOR EACH ROW EXECUTE FUNCTION enforce_workspace_member_role();

CREATE OR REPLACE FUNCTION enforce_workspace_invitation_role()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.member_role_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM family_roles r
        WHERE r.id = NEW.member_role_id AND r.family_id = NEW.workspace_id
    ) THEN
        RAISE EXCEPTION 'Invitation role belongs to another workspace'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM workspace_invitations i
        LEFT JOIN family_roles r ON r.id = i.member_role_id AND r.family_id = i.workspace_id
        WHERE i.member_role_id IS NOT NULL AND r.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Existing invitation role/workspace mismatch';
    END IF;
END;
$$;

CREATE TRIGGER trg_workspace_invitation_role
BEFORE INSERT OR UPDATE OF member_role_id, workspace_id
ON workspace_invitations
FOR EACH ROW EXECUTE FUNCTION enforce_workspace_invitation_role();
