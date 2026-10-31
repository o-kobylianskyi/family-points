package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.TaskInstance;
import com.olehkobylianskyi.familypoints.entity.TaskInstanceStatus;
import com.olehkobylianskyi.familypoints.repository.TaskInstanceRepository;
import com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.TaskScheduleSlotRepository;
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
    private final TaskScheduleSlotRepository slots;
    private final WorkspaceEpochTransactionGuard epochGuard;
    private final org.springframework.transaction.support.TransactionTemplate transactions;

    public TaskDeadlineService(
            TaskInstanceRepository taskInstanceRepository,
            TaskService taskService,
            TaskDefinitionRepository definitions,
            WorkspaceRepository workspaces,
            TaskGenerationService generation,
            TaskScheduleSlotRepository slots,
            WorkspaceEpochTransactionGuard epochGuard,
            org.springframework.transaction.PlatformTransactionManager transactionManager
    ) {
        this.taskInstanceRepository = taskInstanceRepository;
        this.taskService = taskService;
        this.definitions = definitions;
        this.workspaces = workspaces;
        this.generation = generation;
        this.slots = slots;
        this.epochGuard = epochGuard;
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }

    /**
     * Catch up at most one preceding day after downtime without mass-generating future tasks.
     * Mandatory tasks without an explicit due time expire at the end of their scheduled day.
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
        // TransactionTemplate also covers the scheduler's same-bean call.
        // Discovery, materialization and penalty posting use one locked generation.
        return transactions.execute(status -> {
            epochGuard.requireCurrentRequestOrSystemEpoch(workspaceId);
            return processDeadlinesLocked(workspaceId, now);
        });
    }

    private List<TaskInstance> processDeadlinesLocked(Long workspaceId, LocalDateTime now) {
        for (TaskDefinition definition : definitions.findByWorkspaceIdAndActiveTrue(workspaceId)) {
            if (!definition.isMandatory() || definition.getAssignedMember() == null) continue;
            for (int daysAgo = 1; daysAgo >= 0; daysAgo--) {
                LocalDate date = now.toLocalDate().minusDays(daysAgo);
                if (!generation.shouldGenerate(definition, date)) continue;
                var definedSlots = slots.findByTaskDefinitionIdAndEnabledTrueOrderByScheduledTimeAscSlotKeyAsc(definition.getId());
                if (definedSlots.isEmpty() && slots.findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(definition.getId()).isEmpty()) {
                    if (now.isBefore(deadlineFor(date, definition.getDueTime()))) continue;
                    generation.generateForDefinition(definition, date);
                } else {
                    for (var slot : definedSlots) {
                        if (slot.getScheduledTime() == null || now.isBefore(deadlineFor(date, slot.getScheduledTime()))) continue;
                        // Lock the definition before creating one slot; never create future instances.
                        taskService.materializeOverdueSlot(workspaceId, definition.getId(), date, slot.getSlotKey());
                    }
                }
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

            LocalDateTime deadline = deadlineFor(instance.getScheduledDate(), instance.getDueTime());

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
    static LocalDateTime deadlineFor(LocalDate date, java.time.LocalTime dueTime) {
        return dueTime == null ? date.plusDays(1).atStartOfDay() : date.atTime(dueTime);
    }
}
