package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    List<WorkspaceMember> findByWorkspaceIdAndActiveTrue(Long workspaceId);
    Optional<WorkspaceMember> findByIdAndWorkspaceId(Long id, Long workspaceId);
    Optional<WorkspaceMember> findByIdAndWorkspaceIdAndActiveTrue(Long id, Long workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m from WorkspaceMember m
            where m.id = :memberId and m.workspace.id = :workspaceId and m.active = true
            """)
    Optional<WorkspaceMember> findByIdAndWorkspaceIdForUpdate(@Param("memberId") Long memberId, @Param("workspaceId") Long workspaceId);
}
