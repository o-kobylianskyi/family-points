package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.GroupPermission;
import com.olehkobylianskyi.familypoints.entity.RolePermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionGrantRepository extends JpaRepository<RolePermissionGrant, Long> {
    List<RolePermissionGrant> findByRoleDefinitionIdAndPermissionAndActiveTrue(Long roleDefinitionId, GroupPermission permission);
    List<RolePermissionGrant> findByRoleDefinitionIdAndActiveTrueOrderByIdAsc(Long roleDefinitionId);
    Optional<RolePermissionGrant> findByLegacyGroupPermissionGrantId(Long legacyGroupPermissionGrantId);
}
