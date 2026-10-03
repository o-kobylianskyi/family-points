package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.TaskInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TaskGenerationService {

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskInstanceRepository taskInstanceRepository;

    public TaskGenerationService(
            TaskDefinitionRepository taskDefinitionRepository,
            TaskInstanceRepository taskInstanceRepository
    ) {
        this.taskDefinitionRepository = taskDefinitionRepository;
        this.taskInstanceRepository = taskInstanceRepository;
    }

    @Transactional
    public List<TaskInstance> generateForWorkspace(
            Long workspaceId,
            LocalDate date
    ) {
        List<TaskDefinition> definitions =
                taskDefinitionRepository
                        .findByWorkspaceIdAndActiveTrue(workspaceId);

        List<TaskInstance> created = new ArrayList<>();

        for (TaskDefinition definition : definitions) {
            generateForDefinition(definition, date).ifPresent(created::add);
        }

        return created;
    }

    @Transactional
    public Optional<TaskInstance> generateForDefinition(
            TaskDefinition definition,
            LocalDate date
    ) {
        if (definition == null || date == null || !definition.isActive()) {
            return Optional.empty();
        }

        if (!shouldGenerate(definition, date)) {
            return Optional.empty();
        }

        // v1 execution still creates instances only for a concrete member.
        // OPEN/GROUP policies are domain-ready but their claim/participant execution comes next.
        WorkspaceMember member = definition.getAssignedMember();
        if (member == null) {
            return Optional.empty();
        }

        boolean exists = taskInstanceRepository
                .existsByTaskDefinitionIdAndMemberIdAndScheduledDate(
                        definition.getId(), member.getId(), date
                );
        if (exists) {
            return Optional.empty();
        }

        return Optional.of(taskInstanceRepository.save(
                createInstance(definition, member, date)
        ));
    }

    public boolean shouldGenerate(
            TaskDefinition definition,
            LocalDate date
    ) {
        LocalDate startDate = definition.getStartDate();
        LocalDate endDate = definition.getEndDate();

        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }

        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }

        return switch (definition.getRecurrenceType()) {

            case DAILY -> true;

            case WEEKLY ->
                    definition.getRecurrenceDayOfWeek() != null
                            && definition.getRecurrenceDayOfWeek()
                            == date.getDayOfWeek().getValue();

            case MONTHLY ->
                    definition.getRecurrenceDayOfMonth() != null
                            && definition.getRecurrenceDayOfMonth()
                            == date.getDayOfMonth();

            case ONCE ->
                    startDate != null
                            && startDate.equals(date);

            case CUSTOM -> false;
        };
    }

    private TaskInstance createInstance(
            TaskDefinition definition,
            WorkspaceMember member,
            LocalDate date
    ) {
        return new TaskInstance(
                definition,
                member,
                date
        );
    }
}