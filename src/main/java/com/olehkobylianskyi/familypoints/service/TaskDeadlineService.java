package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.TaskInstance;
import com.olehkobylianskyi.familypoints.entity.TaskInstanceStatus;
import com.olehkobylianskyi.familypoints.repository.TaskInstanceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaskDeadlineService {

    private final TaskInstanceRepository taskInstanceRepository;
    private final TaskService taskService;

    public TaskDeadlineService(
            TaskInstanceRepository taskInstanceRepository,
            TaskService taskService
    ) {
        this.taskInstanceRepository = taskInstanceRepository;
        this.taskService = taskService;
    }

    public List<TaskInstance> processDeadlines(
            Long workspaceId,
            LocalDateTime now
    ) {
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
                    taskService.miss(
                            workspaceId,
                            instance.getId()
                    );

            processed.add(result);
        }

        return processed;
    }
}