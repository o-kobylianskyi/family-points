package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.TaskScheduleSlot;
import com.olehkobylianskyi.familypoints.entity.TaskAuditEventType;
import com.olehkobylianskyi.familypoints.repository.TaskScheduleSlotRepository;
import com.olehkobylianskyi.familypoints.service.TaskAuthorizationService;
import com.olehkobylianskyi.familypoints.service.TaskAuditService;
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
    private final TaskAuditService audit;

    public TaskScheduleSlotController(TaskService tasks, TaskScheduleSlotRepository slots,
                                      TaskAuthorizationService authorization, TaskAuditService audit) {
        this.tasks = tasks;
        this.slots = slots;
        this.authorization = authorization;
        this.audit = audit;
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
        var slot = slots.save(new TaskScheduleSlot(definition, input.slotKey(), input.scheduledTime()));
        audit.recordUserEvent(definition, null, TaskAuditEventType.DEFINITION_UPDATED,
                authorization.currentMember(), null, null, null, null,
                "Schedule slot created: " + slot.getSlotKey() + " at " + slot.getScheduledTime());
        return SlotResponse.from(slot);
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
        var oldTime = slot.getScheduledTime();
        var oldEnabled = slot.isEnabled();
        slot.reschedule(input.scheduledTime());
        slot.setEnabled(input.enabled());
        var saved = slots.save(slot);
        if (!java.util.Objects.equals(oldTime, saved.getScheduledTime()) || oldEnabled != saved.isEnabled()) {
            audit.recordUserEvent(definition, null, TaskAuditEventType.DEFINITION_UPDATED,
                    authorization.currentMember(), null, null, null, null,
                    "Schedule slot " + slot.getSlotKey() + ": " + oldTime + " -> " + saved.getScheduledTime()
                            + ", enabled " + oldEnabled + " -> " + saved.isEnabled());
        }
        return SlotResponse.from(saved);
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
        boolean previous = slot.isEnabled();
        slot.setEnabled(enabled);
        var saved = slots.save(slot);
        if (previous != enabled) {
            audit.recordUserEvent(definition, null, TaskAuditEventType.DEFINITION_UPDATED,
                    authorization.currentMember(), null, null, null, null,
                    "Schedule slot " + slot.getSlotKey() + " enabled " + previous + " -> " + enabled);
        }
        return SlotResponse.from(saved);
    }
}
