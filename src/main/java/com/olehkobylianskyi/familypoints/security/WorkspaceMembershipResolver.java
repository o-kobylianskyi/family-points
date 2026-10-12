package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Resolves an explicit workspace only after checking the logged-in account's
 * actual membership. This class does not alter the current legacy JWT context;
 * switching cannot be enabled until every permission evaluator uses this member.
 */
@Service
public class WorkspaceMembershipResolver {
    private final AccountWorkspaceMembershipRepository memberships;

    public WorkspaceMembershipResolver(AccountWorkspaceMembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireActiveMember(UserAccount account, Long workspaceId) {
        if (account == null || account.getId() == null || !account.isEnabled()
                || workspaceId == null || workspaceId <= 0) {
            throw new ResponseStatusException(FORBIDDEN, "Workspace access denied");
        }

        var link = memberships.findByAccountIdAndWorkspaceMemberWorkspaceId(
                account.getId(), workspaceId)
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Workspace access denied"));

        var member = link.getWorkspaceMember();
        if (member == null || !member.isActive() || member.getDeletedAt() != null
                || member.getWorkspace() == null
                || !workspaceId.equals(member.getWorkspace().getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Workspace access denied");
        }
        return member;
    }
}
