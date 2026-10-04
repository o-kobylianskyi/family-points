package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, Long> {
    List<RoleAssignment> findByWorkspaceIdAndActorTypeAndActorIdAndActiveTrue(Long workspaceId, ActorType actorType, Long actorId);
    List<RoleAssignment> findByWorkspaceIdAndContextTypeAndContextIdAndActiveTrue(Long workspaceId, RoleContextType contextType, Long contextId);
    Optional<RoleAssignment> findByWorkspaceIdAndRoleDefinitionIdAndActorTypeAndActorIdAndContextTypeAndContextId(
            Long workspaceId,
            Long roleDefinitionId,
            ActorType actorType,
            Long actorId,
            RoleContextType contextType,
            Long contextId
    );
}
