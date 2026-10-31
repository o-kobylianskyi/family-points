package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRoleRepository extends JpaRepository<WorkspaceRole, Long> {

    List<WorkspaceRole> findByWorkspaceIdOrderByIdAsc(Long workspaceId);

    Optional<WorkspaceRole> findByIdAndWorkspaceId(
            Long id,
            Long workspaceId
    );

    Optional<WorkspaceRole> findByWorkspaceIdAndCode(
            Long workspaceId,
            String code
    );

    boolean existsByWorkspaceIdAndCode(
            Long workspaceId,
            String code
    );
}