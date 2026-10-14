package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from UserAccount a where a.id = :id")
    Optional<UserAccount> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

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