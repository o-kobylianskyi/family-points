package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserAccount> findByWorkspaceMemberId(Long workspaceMemberId);

    boolean existsByWorkspaceMemberId(Long workspaceMemberId);

    List<UserAccount> findAllByWorkspaceMemberWorkspaceIdOrderByWorkspaceMemberIdAsc(Long workspaceId);

    @EntityGraph(attributePaths = {
            "workspaceMember",
            "workspaceMember.workspace",
            "workspaceMember.workspaceRole",
            "workspaceMember.workspaceRole.permissions"
    })
    Optional<UserAccount> findWithSecurityDataByUsername(String username);
}