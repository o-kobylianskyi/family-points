package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.controller.*;
import com.olehkobylianskyi.familypoints.entity.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RemainingWorkspaceEpochBoundaryTest {
    @Test void everyRemainingWorkspaceTransactionFencesBeforeRepositoryAccess() throws Exception {
        var guard = mock(WorkspaceEpochTransactionGuard.class);
        var stale = new IllegalStateException("epoch fence fixture");
        when(guard.requireCurrentRequestEpoch(7L)).thenThrow(stale);
        when(guard.requireCurrentRequestOrLockedEpoch(7L)).thenThrow(stale);
        when(guard.lockCurrentEpoch(7L)).thenThrow(stale);
        int checked = 0;
        for (Class<?> type : List.of(MemberGroupService.class, RoleDefinitionService.class,
                RoleSetService.class, MemberExceptionPeriodService.class, EconomyService.class,
                MediaService.class, WorkspaceMemberService.class, WorkspaceService.class, CalendarService.class,
                WorkspaceRoleService.class, RoleCatalogBootstrapService.class, LegacyGroupRoleBridgeService.class,
                TaskSuggestionController.class, EconomyController.class, WorkspaceTemplateController.class)) {
            var constructor = Arrays.stream(type.getConstructors())
                    .max(Comparator.comparingInt(value -> value.getParameterCount())).orElseThrow();
            var dependencies = new ArrayList<Object>();
            var args = new ArrayList<Object>();
            for (var parameter : constructor.getParameterTypes()) {
                Object dependency = parameter == WorkspaceEpochTransactionGuard.class ? guard : mock(parameter);
                args.add(dependency);
                if (dependency != guard) dependencies.add(dependency);
            }
            var target = constructor.newInstance(args.toArray());
            for (var method : type.getDeclaredMethods()) {
                var annotation = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
                if (annotation == null || annotation.readOnly()) continue;
                Object[] inputs = new Object[method.getParameterCount()];
                Class<?> first = method.getParameterTypes()[0];
                if (first == long.class || first == Long.class) inputs[0] = 7L;
                else if (first == Workspace.class) {
                    var input = mock(Workspace.class); when(input.getId()).thenReturn(7L); inputs[0] = input;
                } else if (first == GroupRole.class) {
                    var input = mock(GroupRole.class, RETURNS_DEEP_STUBS);
                    when(input.getMemberGroup().getWorkspace().getId()).thenReturn(7L); inputs[0] = input;
                } else if (first == GroupPermissionGrant.class) {
                    var input = mock(GroupPermissionGrant.class, RETURNS_DEEP_STUBS);
                    when(input.getGroupRole().getMemberGroup().getWorkspace().getId()).thenReturn(7L); inputs[0] = input;
                } else if (first == GroupMembership.class) {
                    var input = mock(GroupMembership.class, RETURNS_DEEP_STUBS);
                    when(input.getMemberGroup().getWorkspace().getId()).thenReturn(7L); inputs[0] = input;
                } else fail("Unknown writer scope: " + method);
                for (int i=1; i<inputs.length; i++) {
                    var parameter = method.getParameterTypes()[i];
                    if (parameter == boolean.class) inputs[i] = false;
                    else if (parameter == int.class) inputs[i] = 0;
                    else if (parameter == long.class) inputs[i] = 0L;
                }
                var error = assertThrows(InvocationTargetException.class, () -> method.invoke(target, inputs));
                assertSame(stale, error.getCause(), type.getSimpleName() + "." + method.getName());
                checked++;
            }
            for (Object dependency : dependencies) verifyNoInteractions(dependency);
        }
        assertEquals(56, checked);
    }

    @Test void startupMigrationFencesEachFamilyBeforeReadingTaskState() {
        var definitions = mock(com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository.class);
        var nodes = mock(com.olehkobylianskyi.familypoints.repository.WorkNodeRepository.class);
        var participants = mock(com.olehkobylianskyi.familypoints.repository.TaskParticipantRepository.class);
        var workspaces = mock(com.olehkobylianskyi.familypoints.repository.WorkspaceRepository.class);
        var guard = mock(WorkspaceEpochTransactionGuard.class);
        var manager = mock(org.springframework.transaction.PlatformTransactionManager.class);
        var workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(7L);
        when(workspaces.findAll()).thenReturn(List.of(workspace));
        when(guard.lockCurrentEpoch(7L)).thenThrow(new IllegalStateException("fixture"));
        var migration = new DomainFoundationMigrationService(definitions, nodes, participants, guard, workspaces, manager);
        assertThrows(IllegalStateException.class, migration::migrateExistingTaskDefinitions);
        verifyNoInteractions(definitions, nodes, participants);
        verify(manager).rollback(any());
    }

    @Test void legacyRoleBridgeReloadsAndCannotRecreateFromDeletedDetachedRole() {
        var definitions = mock(com.olehkobylianskyi.familypoints.repository.RoleDefinitionRepository.class);
        var assignments = mock(com.olehkobylianskyi.familypoints.repository.RoleAssignmentRepository.class);
        var grants = mock(com.olehkobylianskyi.familypoints.repository.RolePermissionGrantRepository.class);
        var legacy = mock(com.olehkobylianskyi.familypoints.repository.GroupRoleRepository.class);
        var guard = mock(WorkspaceEpochTransactionGuard.class);
        var input = mock(GroupRole.class, RETURNS_DEEP_STUBS);
        when(input.getId()).thenReturn(11L);
        when(input.getMemberGroup().getWorkspace().getId()).thenReturn(7L);
        when(legacy.findById(11L)).thenReturn(Optional.empty());
        var bridge = new LegacyGroupRoleBridgeService(definitions, assignments, grants, guard, legacy,
                mock(com.olehkobylianskyi.familypoints.repository.GroupPermissionGrantRepository.class),
                mock(com.olehkobylianskyi.familypoints.repository.GroupMembershipRepository.class));
        assertThrows(com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException.class,
                () -> bridge.syncRole(input));
        var order = inOrder(guard, legacy);
        order.verify(guard).requireCurrentRequestOrLockedEpoch(7L);
        order.verify(legacy).findById(11L);
        verifyNoInteractions(definitions, assignments, grants);
    }
}
