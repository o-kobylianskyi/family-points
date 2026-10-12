package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.TaskInstance;
import com.olehkobylianskyi.familypoints.entity.TaskInstanceStatus;
import com.olehkobylianskyi.familypoints.repository.TaskInstanceRepository;
import com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceRepository;
import com.olehkobylianskyi.familypoints.entity.TaskDefinition;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskDeadlineService {

    private final TaskInstanceRepository taskInstanceRepository;
    private final TaskService taskService;
    private final TaskDefinitionRepository definitions;
    private final WorkspaceRepository workspaces;
    private final TaskGenerationService generation;

    public TaskDeadlineService(
            TaskInstanceRepository taskInstanceRepository,
            TaskService taskService,
            TaskDefinitionRepository definitions,
            WorkspaceRepository workspaces,
            TaskGenerationService generation
    ) {
        this.taskInstanceRepository = taskInstanceRepository;
        this.taskService = taskService;
        this.definitions = definitions;
        this.workspaces = workspaces;
        this.generation = generation;
    }

    /**
     * Catch up seven days after downtime without mass-generating future tasks.
     * Only tasks with a due time and mandatory=true are automatically penalized.
     */
    @Scheduled(fixedDelayString = "${familypoints.deadlines.poll-ms:300000}")
    public void processAutomaticDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        for (var workspace : workspaces.findAll()) {
            try {
                processDeadlines(workspace.getId(), now);
            } catch (RuntimeException exception) {
                // Keep processing remaining workspaces; scheduled executor logs the error.
                org.slf4j.LoggerFactory.getLogger(TaskDeadlineService.class)
                        .error("Task deadline processing failed for workspace {}", workspace.getId(), exception);
            }
        }
    }

    public List<TaskInstance> processDeadlines(
            Long workspaceId,
            LocalDateTime now
    ) {
        for (TaskDefinition definition : definitions.findByWorkspaceIdAndActiveTrue(workspaceId)) {
            if (!definition.isMandatory() || definition.getDueTime() == null
                    || definition.getAssignedMember() == null) continue;
            for (int daysAgo = 7; daysAgo >= 0; daysAgo--) {
                LocalDate date = now.toLocalDate().minusDays(daysAgo);
                if (now.isBefore(LocalDateTime.of(date, definition.getDueTime()))) continue;
                generation.generateForDefinition(definition, date);
            }
        }

        List<TaskInstance> candidates =
                taskInstanceRepository
                        .findCandidatesForDeadlineProcessing(
                                now.toLocalDate(),
                                List.of(
                                        TaskInstanceStatus.PENDING,
                                        TaskInstanceStatus.IN_PROGRESS
                                )
                        );

        List<TaskInstance> processed =
                new ArrayList<>();

        for (TaskInstance instance : candidates) {

            if (!instance.getMember()
                    .getWorkspace()
                    .getId()
                    .equals(workspaceId)) {
                continue;
            }

            LocalDateTime deadline =
                    LocalDateTime.of(
                            instance.getScheduledDate(),
                            instance.getTaskDefinition()
                                    .getDueTime()
                    );

            if (now.isBefore(deadline)) {
                continue;
            }

            TaskInstance result =
                    taskService.missBySystem(
                            workspaceId,
                            instance.getId()
                    );

            processed.add(result);
        }

        return processed;
    }
}