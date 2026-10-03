package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskDefinitionRepository
        extends JpaRepository<TaskDefinition, Long> {

    List<TaskDefinition>
    findByWorkspaceIdAndActiveTrueOrderByIdAsc(Long workspaceId);

    List<TaskDefinition>
    findByWorkspaceIdOrderByIdAsc(Long workspaceId);

    Optional<TaskDefinition>
    findByIdAndWorkspaceId(Long id, Long workspaceId);

    List<TaskDefinition>
    findByWorkspaceIdAndActiveTrue(Long workspaceId);
}