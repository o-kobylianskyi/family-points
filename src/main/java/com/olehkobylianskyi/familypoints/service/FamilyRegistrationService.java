package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.AccountWorkspaceMembership;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyRegistrationService {
    private final FamilyWorkspaceProvisioningService provisioning;
    private final UserAccountRepository accounts;
    private final WorkspaceMemberRepository members;
    private final AccountWorkspaceMembershipRepository memberships;
    private final PasswordEncoder encoder;

    public FamilyRegistrationService(FamilyWorkspaceProvisioningService provisioning,
            UserAccountRepository accounts, WorkspaceMemberRepository members,
            AccountWorkspaceMembershipRepository memberships, PasswordEncoder encoder) {
        this.provisioning = provisioning;
        this.accounts = accounts;
        this.members = members;
        this.memberships = memberships;
        this.encoder = encoder;
    }

    public record CreatedFamily(Long accountId, Long workspaceId, Long memberId) {}

    @Transactional
    public CreatedFamily register(String username, String password, String familyName, String ownerName) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")
                || password == null || password.length() < 12 || password.length() > 128
                || familyName == null || familyName.isBlank() || familyName.length() > 100
                || ownerName == null || ownerName.isBlank() || ownerName.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration values");
        if (accounts.existsByUsername(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration unavailable");

        var family = provisioning.provision(familyName, ownerName);
        var member = members.findByIdAndWorkspaceId(family.ownerMemberId(), family.workspaceId())
                .orElseThrow();
        var account = accounts.save(new UserAccount(username, encoder.encode(password), member));
        memberships.save(new AccountWorkspaceMembership(account, member));
        return new CreatedFamily(account.getId(), family.workspaceId(), member.getId());
    }
}
