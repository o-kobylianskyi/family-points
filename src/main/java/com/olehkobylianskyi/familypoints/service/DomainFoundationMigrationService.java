package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DomainFoundationMigrationService {
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final WorkNodeRepository workNodeRepository;
    private final TaskParticipantRepository taskParticipantRepository;

    public DomainFoundationMigrationService(TaskDefinitionRepository taskDefinitionRepository,
                                            WorkNodeRepository workNodeRepository,
                                            TaskParticipantRepository taskParticipantRepository) {
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.workNodeRepository = workNodeRepository;
        this.taskParticipantRepository = taskParticipantRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingTaskDefinitions() {
        for (TaskDefinition definition : taskDefinitionRepository.findAll()) {
            boolean changed = false;
            if (definition.getAssignmentPolicy() == null && definition.getAssignedMember() != null) {
                definition.setAssignmentPolicy(AssignmentPolicy.SINGLE_MEMBER); changed = true;
            }
            if (definition.getWorkNode() == null) {
                definition.setWorkNode(workNodeRepository.save(new WorkNode(definition.getWorkspace(), null, WorkNodeType.TASK, definition.getTitle(), definition.getDescription())));
                changed = true;
            }
            if (changed) taskDefinitionRepository.save(definition);

            if (definition.getCreatedBy() != null) add(definition, TaskParticipantRole.ADMIN, ActorType.MEMBER, definition.getCreatedBy().getId());
            if (definition.getAssignedMember() != null) add(definition, TaskParticipantRole.EXECUTOR, ActorType.MEMBER, definition.getAssignedMember().getId());
            if (definition.getTargetGroup() != null && (definition.getAssignmentPolicy()==AssignmentPolicy.GROUP_SHARED || definition.getAssignmentPolicy()==AssignmentPolicy.OPEN_GROUP))
                add(definition, TaskParticipantRole.EXECUTOR, ActorType.GROUP, definition.getTargetGroup().getId());
        }
    }

    private void add(TaskDefinition d, TaskParticipantRole role, ActorType type, Long actorId) {
        if (!taskParticipantRepository.existsByTaskDefinitionIdAndRoleAndActorTypeAndActorId(d.getId(), role, type, actorId))
            taskParticipantRepository.save(new TaskParticipant(d, role, type, actorId));
    }
}
