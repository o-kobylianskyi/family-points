package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskParticipantRepository extends JpaRepository<TaskParticipant, Long> {
    List<TaskParticipant> findByTaskDefinitionIdOrderByIdAsc(Long taskDefinitionId);
    List<TaskParticipant> findByTaskDefinitionIdAndRoleOrderByIdAsc(Long taskDefinitionId, TaskParticipantRole role);
    boolean existsByTaskDefinitionIdAndRoleAndActorTypeAndActorId(Long definitionId, TaskParticipantRole role, ActorType actorType, Long actorId);
    void deleteByTaskDefinitionId(Long taskDefinitionId);
}
