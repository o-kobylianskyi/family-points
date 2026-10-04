package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.RoleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleDefinitionRepository extends JpaRepository<RoleDefinition, Long> {
    List<RoleDefinition> findByWorkspaceIdAndActiveTrueOrderByNameAsc(Long workspaceId);
    List<RoleDefinition> findByRoleSetIdAndActiveTrueOrderByNameAsc(Long roleSetId);
    Optional<RoleDefinition> findByIdAndWorkspaceId(Long id, Long workspaceId);
    Optional<RoleDefinition> findByWorkspaceIdAndSystemCode(Long workspaceId, String systemCode);
    Optional<RoleDefinition> findByLegacyGroupRoleId(Long legacyGroupRoleId);
}
