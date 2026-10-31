package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.AccountWorkspaceMembership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountWorkspaceMembershipRepository extends JpaRepository<AccountWorkspaceMembership, Long> {
    @EntityGraph(attributePaths = {"workspaceMember", "workspaceMember.workspace"})
    List<AccountWorkspaceMembership> findAllByAccountIdOrderByIdAsc(Long accountId);

    @EntityGraph(attributePaths = {"workspaceMember", "workspaceMember.workspace"})
    Optional<AccountWorkspaceMembership> findByAccountIdAndWorkspaceMemberWorkspaceId(Long accountId, Long workspaceId);

    boolean existsByAccountIdAndWorkspaceMemberWorkspaceId(Long accountId, Long workspaceId);
    boolean existsByWorkspaceMemberId(Long workspaceMemberId);

    @EntityGraph(attributePaths = {"account", "workspaceMember"})
    List<AccountWorkspaceMembership> findAllByWorkspaceId(Long workspaceId);
}
