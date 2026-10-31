package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates another family for an existing verified account, without changing
 * its legacy primary family. New member rights are workspace-specific.
 */
@Service
public class AdditionalFamilyService {
    private final CurrentUserService currentUser;
    private final FamilyWorkspaceProvisioningService provisioning;
    private final WorkspaceMemberRepository members;
    private final AccountWorkspaceMembershipRepository memberships;

    public AdditionalFamilyService(CurrentUserService currentUser,
            FamilyWorkspaceProvisioningService provisioning,
            WorkspaceMemberRepository members, AccountWorkspaceMembershipRepository memberships) {
        this.currentUser = currentUser;
        this.provisioning = provisioning;
        this.members = members;
        this.memberships = memberships;
    }

    public record Created(Long workspaceId, Long memberId) {}

    @Transactional
    public Created create(String familyName, String ownerName) {
        var account = currentUser.getCurrentAccount();
        if (!account.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var family = provisioning.provision(familyName, ownerName);
        var owner = members.findByIdAndWorkspaceId(family.ownerMemberId(), family.workspaceId())
                .orElseThrow();
        memberships.save(new AccountWorkspaceMembership(account, owner));
        return new Created(family.workspaceId(), family.ownerMemberId());
    }
}
