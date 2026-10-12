package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.TaskScheduleSlot;
import com.olehkobylianskyi.familypoints.repository.TaskScheduleSlotRepository;
import com.olehkobylianskyi.familypoints.service.TaskAuthorizationService;
import com.olehkobylianskyi.familypoints.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/** Slot definitions only; no rewards or dated instances are created here. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/tasks/definitions/{definitionId}/schedule-slots")
public class TaskScheduleSlotController {
    private final TaskService tasks;
    private final TaskScheduleSlotRepository slots;
    private final TaskAuthorizationService authorization;

    public TaskScheduleSlotController(TaskService tasks, TaskScheduleSlotRepository slots,
                                      TaskAuthorizationService authorization) {
        this.tasks = tasks;
        this.slots = slots;
        this.authorization = authorization;
    }

    public record SlotRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String slotKey,
            LocalTime scheduledTime) {}

    public record SlotResponse(Long id, String slotKey, LocalTime scheduledTime, boolean enabled) {
        public static SlotResponse from(TaskScheduleSlot slot) {
            return new SlotResponse(slot.getId(), slot.getSlotKey(), slot.getScheduledTime(), slot.isEnabled());
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SlotResponse> list(@PathVariable Long workspaceId, @PathVariable Long definitionId) {
        var definition = tasks.getDefinition(workspaceId, definitionId);
        authorization.requireRead(definition);
        return slots.findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(definitionId)
                .stream().map(SlotResponse::from).toList();
    }

    @PostMapping
    @Transactional
    public SlotResponse create(@PathVariable Long workspaceId, @PathVariable Long definitionId,
                               @Valid @RequestBody SlotRequest input) {
        var definition = tasks.getDefinition(workspaceId, definitionId);
        authorization.requireManage(definition);
        if ("default".equals(input.slotKey())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Reserved schedule slot key");
        }
        return SlotResponse.from(slots.save(new TaskScheduleSlot(
                definition, input.slotKey(), input.scheduledTime())));
    }

    public record SlotUpdateRequest(LocalTime scheduledTime, boolean enabled) {}

    @PutMapping("/{slotId}")
    @Transactional
    public SlotResponse update(@PathVariable Long workspaceId, @PathVariable Long definitionId,
                               @PathVariable Long slotId, @RequestBody SlotUpdateRequest input) {
        var definition = tasks.getDefinition(workspaceId, definitionId);
        authorization.requireManage(definition);
        var slot = slots.findByIdAndTaskDefinitionId(slotId, definitionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Schedule slot not found"));
        // Keep identity stable: completed/missed historical instances retain their own dueTime snapshot.
        slot.reschedule(input.scheduledTime());
        slot.setEnabled(input.enabled());
        return SlotResponse.from(slots.save(slot));
    }

    @PatchMapping("/{slotId}/enabled")
    @Transactional
    public SlotResponse setEnabled(@PathVariable Long workspaceId, @PathVariable Long definitionId,
                                   @PathVariable Long slotId, @RequestParam boolean enabled) {
        var definition = tasks.getDefinition(workspaceId, definitionId);
        authorization.requireManage(definition);
        var slot = slots.findByIdAndTaskDefinitionId(slotId, definitionId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Schedule slot not found"));
        slot.setEnabled(enabled);
        return SlotResponse.from(slots.save(slot));
    }
}
