package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskRewardEpochBoundaryTest {
    @Test void everyTransactionalTaskRewardEntryRejectsBeforeDomainAccess() throws Exception {
        var guard = mock(WorkspaceEpochTransactionGuard.class);
        var stale = new IllegalStateException("stale epoch fixture");
        when(guard.requireCurrentRequestEpoch(7L)).thenThrow(stale);
        when(guard.requireCurrentRequestOrSystemEpoch(7L)).thenThrow(stale);
        when(guard.lockCurrentEpoch(7L)).thenThrow(stale);
        int checked = 0;
        for (Class<?> type : List.of(TaskService.class, RewardService.class,
                TaskRewardNegotiationService.class, RewardAvailabilityService.class, TaskGenerationService.class)) {
            var constructor = type.getConstructors()[0];
            var dependencies = new ArrayList<Object>();
            var args = new ArrayList<Object>();
            for (Class<?> parameter : constructor.getParameterTypes()) {
                Object dependency = parameter == WorkspaceEpochTransactionGuard.class ? guard : mock(parameter);
                args.add(dependency);
                if (dependency != guard) dependencies.add(dependency);
            }
            Object service = constructor.newInstance(args.toArray());
            for (var method : type.getDeclaredMethods()) {
                var annotation = method.getAnnotation(Transactional.class);
                if (annotation == null || annotation.readOnly() || method.getName().equals("generateForDefinition")) continue;
                Object[] arguments = new Object[method.getParameterCount()];
                arguments[0] = 7L;
                for (int i = 1; i < arguments.length; i++) {
                    Class<?> parameter = method.getParameterTypes()[i];
                    if (parameter == boolean.class) arguments[i] = false;
                    else if (parameter == int.class) arguments[i] = 0;
                    else if (parameter == long.class) arguments[i] = 0L;
                }
                var error = assertThrows(InvocationTargetException.class, () -> method.invoke(service, arguments),
                        type.getSimpleName() + "." + method.getName());
                assertSame(stale, error.getCause(), type.getSimpleName() + "." + method.getName());
                checked++;
            }
            for (Object dependency : dependencies) verifyNoInteractions(dependency);
        }
        assertEquals(38, checked, "Include every new task/reward mutation in the epoch boundary audit");
    }

    @Test void generationReloadsDetachedDefinitionAfterLockAndCannotResurrectDeletedTask() {
        var definitions = mock(com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository.class);
        var instances = mock(com.olehkobylianskyi.familypoints.repository.TaskInstanceRepository.class);
        var audit = mock(TaskAuditService.class);
        var slots = mock(com.olehkobylianskyi.familypoints.repository.TaskScheduleSlotRepository.class);
        var guard = mock(WorkspaceEpochTransactionGuard.class);
        var definition = mock(com.olehkobylianskyi.familypoints.entity.TaskDefinition.class);
        var workspace = mock(com.olehkobylianskyi.familypoints.entity.Workspace.class);
        when(definition.getId()).thenReturn(11L);
        when(definition.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(7L);
        when(definitions.findForOccurrenceUpdate(11L, 7L)).thenReturn(Optional.empty());
        var service = new TaskGenerationService(definitions, instances, audit, slots, guard);
        assertTrue(service.generateForDefinition(definition, java.time.LocalDate.now()).isEmpty());
        var order = inOrder(guard, definitions);
        order.verify(guard).requireCurrentRequestOrSystemEpoch(7L);
        order.verify(definitions).findForOccurrenceUpdate(11L, 7L);
        verifyNoInteractions(instances, audit, slots);
    }

    @Test void deadlineScanDoesNotReadCandidatesBeforeItsTransactionFence() {
        var instances = mock(com.olehkobylianskyi.familypoints.repository.TaskInstanceRepository.class);
        var tasks = mock(TaskService.class);
        var definitions = mock(com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository.class);
        var workspaces = mock(com.olehkobylianskyi.familypoints.repository.WorkspaceRepository.class);
        var generation = mock(TaskGenerationService.class);
        var slots = mock(com.olehkobylianskyi.familypoints.repository.TaskScheduleSlotRepository.class);
        var guard = mock(WorkspaceEpochTransactionGuard.class);
        var manager = mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(guard.requireCurrentRequestOrSystemEpoch(7L)).thenThrow(new IllegalStateException("stale"));
        var service = new TaskDeadlineService(instances, tasks, definitions, workspaces, generation, slots, guard, manager);
        assertThrows(IllegalStateException.class, () -> service.processDeadlines(7L, java.time.LocalDateTime.now()));
        verifyNoInteractions(instances, tasks, definitions, workspaces, generation, slots);
        verify(manager).rollback(any());
    }
}
