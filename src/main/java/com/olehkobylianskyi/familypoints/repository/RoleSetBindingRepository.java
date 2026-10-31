package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoleSetBindingRepository extends JpaRepository<RoleSetBinding, Long> {
    List<RoleSetBinding> findByRoleSetId(Long roleSetId);
    List<RoleSetBinding> findByContextTypeAndContextId(RoleContextType contextType, Long contextId);
}
