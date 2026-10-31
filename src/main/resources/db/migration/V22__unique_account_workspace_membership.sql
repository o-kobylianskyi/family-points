-- Prevent concurrent invitations from attaching one login to multiple members
-- of the same workspace. Preserve distinct workspaces per global account.
-- Existing duplicate rows (if any) must be resolved explicitly, not silently deleted.
ALTER TABLE account_workspace_memberships
    ADD COLUMN workspace_id BIGINT;

UPDATE account_workspace_memberships awm
SET workspace_id = fm.family_id
FROM family_members fm
WHERE fm.id = awm.family_member_id;

ALTER TABLE account_workspace_memberships
    ALTER COLUMN workspace_id SET NOT NULL,
    ADD CONSTRAINT fk_account_membership_workspace
        FOREIGN KEY (workspace_id) REFERENCES families(id) ON DELETE RESTRICT,
    ADD CONSTRAINT uq_account_workspace_membership
        UNIQUE (account_id, workspace_id);

-- Keep the workspace ID equal to the referenced member's workspace, even when
-- SQL clients bypass the application. No update may silently move a membership.
CREATE OR REPLACE FUNCTION enforce_account_membership_workspace()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM family_members fm
        WHERE fm.id = NEW.family_member_id AND fm.family_id = NEW.workspace_id
    ) THEN
        RAISE EXCEPTION 'Membership workspace does not match member workspace'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_account_membership_workspace
    BEFORE INSERT OR UPDATE OF family_member_id, workspace_id
    ON account_workspace_memberships
    FOR EACH ROW EXECUTE FUNCTION enforce_account_membership_workspace();
