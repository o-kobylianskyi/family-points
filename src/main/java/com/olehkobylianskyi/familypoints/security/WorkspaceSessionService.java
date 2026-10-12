package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Build a workspace-bound identity from a verified database membership.
 *
 * This is a preparation-only service: scoped JWT authentication is still disabled
 * by JwtAuthenticationFilter. Never derive permissions from the primary member
 * when the requested workspace differs.
 */
@Service
public class WorkspaceSessionService {
    private final WorkspaceContextAuthorization authorization;

    public WorkspaceSessionService(WorkspaceContextAuthorization authorization) {
        this.authorization = authorization;
    }

    public record WorkspaceSession(
            Long accountId, String username, Long workspaceId, Long memberId,
            Set<WorkspacePermission> permissions) {
        public WorkspaceSession {
            permissions = Set.copyOf(permissions);
        }

        public boolean hasPermission(WorkspacePermission permission) {
            return permission != null && (permissions.contains(permission)
                    || permissions.contains(WorkspacePermission.ADMIN_OVERRIDE));
        }
    }

    @Transactional(readOnly = true)
    public WorkspaceSession requireSession(UserAccount account, Long workspaceId) {
        var member = authorization.requireActiveContext(account, workspaceId);
        return new WorkspaceSession(
                account.getId(),
                account.getUsername(),
                workspaceId,
                member.getId(),
                member.getWorkspaceRole().getPermissions()
        );
    }
}
