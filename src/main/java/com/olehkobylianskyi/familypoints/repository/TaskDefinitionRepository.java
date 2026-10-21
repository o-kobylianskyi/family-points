package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TaskDefinitionRepository
        extends JpaRepository<TaskDefinition, Long> {

    List<TaskDefinition>
    findByWorkspaceIdAndActiveTrueOrderByIdAsc(Long workspaceId);

    List<TaskDefinition>
    findByWorkspaceIdOrderByIdAsc(Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    Optional<TaskDefinition>
    findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from TaskDefinition d where d.id = :id and d.workspace.id = :workspaceId")
    Optional<TaskDefinition> findForOccurrenceUpdate(@Param("id") Long id,
                                                     @Param("workspaceId") Long workspaceId);

    Optional<TaskDefinition> findByWorkNodeId(Long workNodeId);

    List<TaskDefinition>
    findByWorkspaceIdAndActiveTrue(Long workspaceId);
}