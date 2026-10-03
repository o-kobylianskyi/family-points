package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.service.TaskDeadlineService;
import com.olehkobylianskyi.familypoints.service.TaskGenerationService;
import com.olehkobylianskyi.familypoints.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskGenerationService taskGenerationService;
    private final TaskDeadlineService taskDeadlineService;

    public TaskController(
            TaskService taskService,
            TaskGenerationService taskGenerationService,
            TaskDeadlineService taskDeadlineService
    ) {
        this.taskService = taskService;
        this.taskGenerationService = taskGenerationService;
        this.taskDeadlineService = taskDeadlineService;
    }

    @PostMapping("/definitions")
    public ResponseEntity<TaskDefinitionResponse> createDefinition(
            @PathVariable Long workspaceId,
            @Valid @RequestBody TaskDefinitionCreateRequest request
    ) {
        TaskDefinition definition =
                taskService.createDefinition(
                        workspaceId,
                        request.getAssignmentPolicy(),
                        request.getAssignedMemberId(),
                        request.getTargetGroupId(),
                        request.getPreferredMemberId(),
                        request.getResponsibleMemberId(),
                        request.isDelegationAllowed(),
                        request.getRoleMatchMode(),
                        request.getRequiredGroupRoleIds(),
                        request.getAdministrators(),
                        request.getObservers(),
                        request.getExecutors(),
                        request.getTitle(),
                        request.getDescription(),
                        request.isMandatory(),
                        request.getRecurrenceType(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getRecurrenceDayOfWeek(),
                        request.getRecurrenceDayOfMonth(),
                        request.getRewardPointTypeId(),
                        request.getRewardAmount(),
                        request.getPenaltyPointTypeId(),
                        request.getPenaltyAmount(),
                        request.getDueTime()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        TaskDefinitionResponse.from(
                                definition
                        )
                );
    }

    @GetMapping("/definitions")
    public List<TaskDefinitionResponse> getDefinitions(
            @PathVariable Long workspaceId
    ) {
        return taskService
                .getDefinitions(workspaceId)
                .stream()
                .map(TaskDefinitionResponse::from)
                .toList();
    }


    @PutMapping("/definitions/{definitionId}")
    public TaskDefinitionResponse updateDefinition(
            @PathVariable Long workspaceId,
            @PathVariable Long definitionId,
            @Valid @RequestBody TaskDefinitionCreateRequest request
    ) {
        return TaskDefinitionResponse.from(taskService.updateDefinition(workspaceId, definitionId, request));
    }

    @PatchMapping("/definitions/{definitionId}/active")
    public TaskDefinitionResponse setDefinitionActive(
            @PathVariable Long workspaceId,
            @PathVariable Long definitionId,
            @RequestParam boolean active
    ) {
        return TaskDefinitionResponse.from(taskService.setDefinitionActive(workspaceId, definitionId, active));
    }

    @GetMapping("/definitions/{definitionId}/participants")
    public List<TaskParticipantResponse> getParticipants(@PathVariable Long workspaceId, @PathVariable Long definitionId) {
        return taskService.getParticipants(workspaceId, definitionId).stream()
                .map(p -> new TaskParticipantResponse(p, taskService.getActorName(workspaceId, p))).toList();
    }

    @GetMapping("/definitions/views/{view}")
    public List<TaskDefinitionResponse> getDefinitionsByView(@PathVariable Long workspaceId, @PathVariable String view) {
        return taskService.getDefinitionsByParticipation(workspaceId, view).stream().map(TaskDefinitionResponse::from).toList();
    }

    @PostMapping("/definitions/{definitionId}/instances")
    public ResponseEntity<TaskInstanceResponse> createInstance(
            @PathVariable Long workspaceId,
            @PathVariable Long definitionId,
            @RequestParam LocalDate date
    ) {
        TaskInstance instance =
                taskService.createInstance(
                        workspaceId,
                        definitionId,
                        date
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        TaskInstanceResponse.from(instance)
                );
    }

    @GetMapping("/members/{memberId}")
    public List<TaskInstanceResponse> getMemberTasks(
            @PathVariable Long workspaceId,
            @PathVariable Long memberId,
            @RequestParam LocalDate date
    ) {
        return taskService
                .getMemberTasks(
                        workspaceId,
                        memberId,
                        date
                )
                .stream()
                .map(TaskInstanceResponse::from)
                .toList();
    }


    @GetMapping("/open")
    public List<TaskDefinitionResponse> getOpenTasks(
            @PathVariable Long workspaceId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long memberId
    ) {
        return taskService.getOpenDefinitions(workspaceId, date, memberId).stream()
                .map(TaskDefinitionResponse::from).toList();
    }

    @PostMapping("/definitions/{definitionId}/claim")
    public TaskInstanceResponse claim(
            @PathVariable Long workspaceId,
            @PathVariable Long definitionId,
            @RequestParam LocalDate date,
            @RequestParam Long memberId
    ) {
        return TaskInstanceResponse.from(taskService.claim(workspaceId, definitionId, date, memberId));
    }

    @PostMapping("/instances/{instanceId}/delegate")
    public TaskInstanceResponse delegate(
            @PathVariable Long workspaceId,
            @PathVariable Long instanceId,
            @Valid @RequestBody TaskDelegationRequest request
    ) {
        return TaskInstanceResponse.from(taskService.delegate(workspaceId, instanceId, request.getToMemberId(), request.getReason()));
    }

    @PostMapping("/instances/{instanceId}/start")
    public TaskInstanceResponse start(
            @PathVariable Long workspaceId,
            @PathVariable Long instanceId
    ) {
        return TaskInstanceResponse.from(
                taskService.start(
                        workspaceId,
                        instanceId
                )
        );
    }

    @PostMapping("/instances/{instanceId}/complete")
    public TaskInstanceResponse complete(
            @PathVariable Long workspaceId,
            @PathVariable Long instanceId
    ) {
        return TaskInstanceResponse.from(
                taskService.complete(
                        workspaceId,
                        instanceId
                )
        );
    }

    @PostMapping("/instances/{instanceId}/miss")
    public TaskInstanceResponse miss(
            @PathVariable Long workspaceId,
            @PathVariable Long instanceId
    ) {
        return TaskInstanceResponse.from(
                taskService.miss(
                        workspaceId,
                        instanceId
                )
        );
    }

    @PostMapping("/instances/{instanceId}/excuse")
    public TaskInstanceResponse excuse(
            @PathVariable Long workspaceId,
            @PathVariable Long instanceId,
            @Valid @RequestBody TaskExcuseRequest request
    ) {
        return TaskInstanceResponse.from(
                taskService.excuse(
                        workspaceId,
                        instanceId,
                        request.getReason(),
                        request.getComment()
                )
        );
    }


    @PostMapping("/generate")
    public List<TaskInstanceResponse> generate(
            @PathVariable Long workspaceId,
            @RequestParam LocalDate date
    ) {
        return taskGenerationService
                .generateForWorkspace(workspaceId, date)
                .stream()
                .map(TaskInstanceResponse::from)
                .toList();
    }

    @PostMapping("/process-deadlines")
    public List<TaskInstanceResponse> processDeadlines(
            @PathVariable Long workspaceId,
            @RequestParam LocalDateTime now
    ) {
        return taskDeadlineService
                .processDeadlines(workspaceId, now)
                .stream()
                .map(TaskInstanceResponse::from)
                .toList();
    }
}