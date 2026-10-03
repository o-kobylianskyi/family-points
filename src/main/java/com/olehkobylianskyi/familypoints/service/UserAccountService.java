package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> getAccounts(Long workspaceId) {
        return userAccountRepository.findAllByWorkspaceMemberWorkspaceIdOrderByWorkspaceMemberIdAsc(workspaceId);
    }

    @Transactional
    public Optional<UserAccount> createAccount(
            Long workspaceId,
            Long workspaceMemberId,
            String username,
            String password
    ) {
        Optional<WorkspaceMember> memberOptional =
                workspaceMemberRepository.findByIdAndWorkspaceId(workspaceMemberId, workspaceId);

        if (memberOptional.isEmpty()) return Optional.empty();
        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userAccountRepository.existsByWorkspaceMemberId(workspaceMemberId)) {
            throw new IllegalArgumentException("Workspace member already has a user account");
        }

        UserAccount account = new UserAccount(
                username,
                passwordEncoder.encode(password),
                memberOptional.get()
        );
        return Optional.of(userAccountRepository.save(account));
    }

    @Transactional
    public Optional<UserAccount> updateAccount(
            Long workspaceId,
            Long workspaceMemberId,
            String username,
            boolean enabled
    ) {
        Optional<UserAccount> accountOptional = findAccount(workspaceId, workspaceMemberId);
        if (accountOptional.isEmpty()) return Optional.empty();

        UserAccount account = accountOptional.get();
        userAccountRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(account.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Username already exists"); });

        account.setUsername(username);
        account.setEnabled(enabled);
        return Optional.of(userAccountRepository.save(account));
    }

    @Transactional
    public boolean resetPassword(Long workspaceId, Long workspaceMemberId, String newPassword) {
        Optional<UserAccount> accountOptional = findAccount(workspaceId, workspaceMemberId);
        if (accountOptional.isEmpty()) return false;
        UserAccount account = accountOptional.get();
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(account);
        return true;
    }

    @Transactional
    public void changeOwnPassword(UserAccount account, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(account);
    }

    private Optional<UserAccount> findAccount(Long workspaceId, Long workspaceMemberId) {
        return userAccountRepository.findByWorkspaceMemberId(workspaceMemberId)
                .filter(account -> account.getWorkspaceMember().getWorkspace().getId().equals(workspaceId));
    }
}
