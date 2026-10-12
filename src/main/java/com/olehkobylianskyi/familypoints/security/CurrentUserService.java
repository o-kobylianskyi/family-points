package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("currentUser")
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;

    public CurrentUserService(
            UserAccountRepository userAccountRepository
    ) {
        this.userAccountRepository = userAccountRepository;
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