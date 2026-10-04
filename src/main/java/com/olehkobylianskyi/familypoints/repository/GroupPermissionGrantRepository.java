package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupPermissionGrantRepository extends JpaRepository<GroupPermissionGrant, Long> {
    List<GroupPermissionGrant> findByGroupRoleIdAndPermission(Long groupRoleId, GroupPermission permission);
}
