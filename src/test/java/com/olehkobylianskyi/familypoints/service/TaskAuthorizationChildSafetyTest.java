package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskAuthorizationChildSafetyTest {
    final CurrentUserService current = mock(CurrentUserService.class);
    final TaskAuthorizationService auth = new TaskAuthorizationService(
            current,
            mock(TaskParticipantRepository.class),
            mock(GroupMembershipRepository.class),
            mock(GroupCompositionRepository.class),
            mock(RoleAssignmentRepository.class),
            mock(RolePermissionGrantRepository.class));

    @Test
    void childCannotMintTasksEvenWithCreatePermission() {
        var child = mock(WorkspaceMember.class);
        when(current.belongsToWorkspace(20L)).thenReturn(true);
        when(current.getCurrentMember()).thenReturn(child);
        when(child.getMemberType()).thenReturn(WorkspaceMemberType.CHILD);
        when(child.hasPermission(WorkspacePermission.CREATE_TASKS)).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> auth.requireCreate(20L));
    }

    @Test
    void childCannotEditPreviouslySelfCreatedTask() {
        var child = mock(WorkspaceMember.class);
        var workspace = mock(Workspace.class);
        var definition = mock(TaskDefinition.class);
        when(current.getCurrentMember()).thenReturn(child);
        when(child.getMemberType()).thenReturn(WorkspaceMemberType.CHILD);
        when(child.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(20L);
        when(definition.getWorkspace()).thenReturn(workspace);
        when(child.hasPermission(WorkspacePermission.MANAGE_TASKS)).thenReturn(true);
        assertFalse(auth.canManage(definition));
    }
}
