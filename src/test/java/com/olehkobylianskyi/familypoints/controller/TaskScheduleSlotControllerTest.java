package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.TaskDefinition;
import com.olehkobylianskyi.familypoints.entity.TaskScheduleSlot;
import com.olehkobylianskyi.familypoints.repository.TaskScheduleSlotRepository;
import com.olehkobylianskyi.familypoints.service.TaskAuthorizationService;
import com.olehkobylianskyi.familypoints.service.TaskService;
import com.olehkobylianskyi.familypoints.service.TaskAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskScheduleSlotControllerTest {
    @Test
    void listingDoesNotMaterializeTaskInstances() {
        var tasks = mock(TaskService.class);
        var repo = mock(TaskScheduleSlotRepository.class);
        var authorization = mock(TaskAuthorizationService.class);
        var definition = mock(TaskDefinition.class);
        when(tasks.getDefinition(8L,39L)).thenReturn(definition);
        when(repo.findByTaskDefinitionIdAndEnabledTrueOrderByScheduledTimeAscSlotKeyAsc(39L))
                .thenReturn(List.of());
        var controller = new TaskScheduleSlotController(tasks, repo, authorization, mock(TaskAuditService.class));
        assertTrue(controller.list(8L,39L).isEmpty());
        verify(authorization).requireRead(definition);
        verify(repo, never()).save(any());
    }

    @Test
    void creationRequiresManagerRightsBeforePersistence() {
        var tasks = mock(TaskService.class);
        var repo = mock(TaskScheduleSlotRepository.class);
        var authorization = mock(TaskAuthorizationService.class);
        var definition = mock(TaskDefinition.class);
        when(tasks.getDefinition(8L,39L)).thenReturn(definition);
        doThrow(new AccessDeniedException("Not a manager")).when(authorization).requireManage(definition);
        var controller = new TaskScheduleSlotController(tasks, repo, authorization, mock(TaskAuditService.class));
        assertThrows(AccessDeniedException.class, () -> controller.create(8L,39L,
                new TaskScheduleSlotController.SlotRequest("morning", LocalTime.of(8,0))));
        verify(repo, never()).save(any());
    }

    @Test
    void updateRequiresManageBeforeAccessingSlot() {
        var tasks = mock(TaskService.class);
        var repo = mock(TaskScheduleSlotRepository.class);
        var authorization = mock(TaskAuthorizationService.class);
        var definition = mock(TaskDefinition.class);
        when(tasks.getDefinition(8L,39L)).thenReturn(definition);
        doThrow(new AccessDeniedException("Denied")).when(authorization).requireManage(definition);
        var controller = new TaskScheduleSlotController(tasks, repo, authorization, mock(TaskAuditService.class));
        assertThrows(AccessDeniedException.class, () -> controller.update(8L,39L,7L,
                new TaskScheduleSlotController.SlotUpdateRequest(LocalTime.of(9,0),false)));
        verifyNoInteractions(repo);
    }

    @Test
    void disabledSlotsRemainListedForManagement() {
        var tasks = mock(TaskService.class);
        var repo = mock(TaskScheduleSlotRepository.class);
        var authorization = mock(TaskAuthorizationService.class);
        var definition = mock(TaskDefinition.class);
        var slot = new TaskScheduleSlot(definition,"morning",LocalTime.of(8,0));
        slot.setEnabled(false);
        when(tasks.getDefinition(8L,39L)).thenReturn(definition);
        when(repo.findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(39L)).thenReturn(List.of(slot));
        var controller = new TaskScheduleSlotController(tasks,repo,authorization,mock(TaskAuditService.class));
        var result = controller.list(8L,39L);
        assertEquals(1,result.size());
        assertFalse(result.getFirst().enabled());
    }
}
