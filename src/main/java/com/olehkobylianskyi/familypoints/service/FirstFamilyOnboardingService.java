package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Creates the first family for a global account while keeping legacy accounts intact. */
@Service
public class FirstFamilyOnboardingService {
    private final CurrentUserService currentUser;
    private final UserAccountRepository accounts;
    private final AccountWorkspaceMembershipRepository memberships;
    private final AdditionalFamilyService families;

    public FirstFamilyOnboardingService(CurrentUserService currentUser,
            UserAccountRepository accounts, AccountWorkspaceMembershipRepository memberships,
            AdditionalFamilyService families) {
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.memberships = memberships;
        this.families = families;
    }

    public record Created(Long workspaceId, Long memberId) {}

    @Transactional
    public Created create(String familyName, String ownerName) {
        var account = currentUser.getCurrentAccount();
        if (!account.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // Serialize concurrent requests for the same global identity.
        var locked = accounts.findByIdForUpdate(account.getId()).orElseThrow();
        if (locked.getWorkspaceMember() != null ||
                !memberships.findAllByAccountIdOrderByIdAsc(account.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "First family already exists");
        }
        var created = families.create(familyName, ownerName);
        return new Created(created.workspaceId(), created.memberId());
    }
}
