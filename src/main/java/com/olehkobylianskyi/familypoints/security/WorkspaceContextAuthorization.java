package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Permission decisions for an explicitly selected workspace.
 * Not wired into legacy request authorization until JWT context is migrated.
 */
@Service
public class WorkspaceContextAuthorization {
    private final WorkspaceMembershipResolver resolver;

    public WorkspaceContextAuthorization(WorkspaceMembershipResolver resolver) {
        this.resolver = resolver;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireActiveContext(UserAccount account, Long workspaceId) {
        WorkspaceMember member = resolver.requireActiveMember(account, workspaceId);
        if (member.getWorkspaceRole() == null
                || member.getWorkspaceRole().getWorkspace() == null
                || !workspaceId.equals(member.getWorkspaceRole().getWorkspace().getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Workspace role mismatch");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UserAccount account, Long workspaceId, WorkspacePermission permission) {
        if (permission == null) return false;
        try {
            WorkspaceMember member = requireActiveContext(account, workspaceId);
            return member.hasPermission(permission)
                    || member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE);
        } catch (ResponseStatusException exception) {
            return false;
        }
    }
}
