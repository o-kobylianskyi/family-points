package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RoleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleSetRepository extends JpaRepository<RoleSet, Long> {
    List<RoleSet> findByWorkspaceIdAndActiveTrueOrderByNameAsc(Long workspaceId);
    Optional<RoleSet> findByIdAndWorkspaceId(Long id, Long workspaceId);
    boolean existsByWorkspaceIdAndNameIgnoreCase(Long workspaceId, String name);
}
