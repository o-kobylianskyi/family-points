package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountMembershipLinkService {
    private final AccountWorkspaceMembershipRepository memberships;
    private final WorkspaceMemberRepository members;

    public AccountMembershipLinkService(AccountWorkspaceMembershipRepository memberships, WorkspaceMemberRepository members) {
        this.memberships = memberships;
        this.members = members;
    }

    @Transactional
    public AccountWorkspaceMembership linkExistingAccount(UserAccount account, Long workspaceId, Long memberId) {
        if (account == null || account.getId() == null || !account.isEnabled()
                || workspaceId == null || memberId == null) {
            throw new IllegalArgumentException("Invalid membership request");
        }
        var member = members.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Active member not found"));
        if (member.getDeletedAt() != null
                || memberships.existsByWorkspaceMemberId(memberId)
                || memberships.existsByAccountIdAndWorkspaceMemberWorkspaceId(account.getId(), workspaceId)) {
            throw new IllegalArgumentException("Membership is unavailable");
        }
        return memberships.save(new AccountWorkspaceMembership(account, member));
    }
}
