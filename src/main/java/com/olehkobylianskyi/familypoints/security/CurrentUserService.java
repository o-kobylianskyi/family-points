package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service("currentUser")
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;
    private final WorkspaceMembershipResolver workspaceMembershipResolver;

    @Autowired
    public CurrentUserService(UserAccountRepository userAccountRepository,
                              WorkspaceMembershipResolver workspaceMembershipResolver) {
        this.userAccountRepository = userAccountRepository;
        this.workspaceMembershipResolver = workspaceMembershipResolver;
    }

    // Compatibility for isolated unit tests that only mock legacy account lookup.
    public CurrentUserService(UserAccountRepository userAccountRepository) {
        this(userAccountRepository, null);
    }

    public UserAccount getCurrentAccount() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        return userAccountRepository
                .findWithSecurityDataByUsername(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user account not found"
                        )
                );
    }

    public com.olehkobylianskyi.familypoints.entity.WorkspaceMember getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof WorkspaceAuthenticationDetails details) {
            UserAccount account = getCurrentAccount();
            if (workspaceMembershipResolver == null || !details.accountId().equals(account.getId())) {
                throw new ResponseStatusException(FORBIDDEN, "Workspace authentication mismatch");
            }
            var member = workspaceMembershipResolver.requireActiveMember(account, details.workspaceId());
            if (!details.memberId().equals(member.getId())) {
                throw new ResponseStatusException(FORBIDDEN, "Workspace member mismatch");
            }
            return member;
        }
        return getCurrentAccount().getWorkspaceMember();
    }

    public Long getCurrentMemberId() {
        return getCurrentMember().getId();
    }

    public Long getCurrentWorkspaceId() {
        return getCurrentMember().getWorkspace().getId();
    }

    public boolean belongsToWorkspace(Long workspaceId) {
        return getCurrentWorkspaceId().equals(workspaceId);
    }
}