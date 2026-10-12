package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.TaskDefinition;
import com.olehkobylianskyi.familypoints.entity.TaskScheduleSlot;
import com.olehkobylianskyi.familypoints.repository.TaskScheduleSlotRepository;
import com.olehkobylianskyi.familypoints.service.TaskAuthorizationService;
import com.olehkobylianskyi.familypoints.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalTime;
import java.util.List;

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
        var controller = new TaskScheduleSlotController(tasks, repo, authorization);
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
        var controller = new TaskScheduleSlotController(tasks, repo, authorization);
        assertThrows(AccessDeniedException.class, () -> controller.create(8L,39L,
                new TaskScheduleSlotController.SlotRequest("morning", LocalTime.of(8,0))));
        verify(repo, never()).save(any());
    }
}
