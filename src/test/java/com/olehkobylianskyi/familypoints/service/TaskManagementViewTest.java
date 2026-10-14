package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskManagementViewTest {
    @Mock WorkspaceRepository workspaces;
    @Mock TaskDefinitionRepository definitions;
    @Mock TaskAuthorizationService authorization;
    @Mock CurrentUserService currentUser;
    @InjectMocks TaskService tasks;

    @Test
    void createdViewDoesNotExposeTasksAuthoredByAnotherMember() throws Exception {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            when(workspaces.findById(5L)).thenReturn(java.util.Optional.of(mock(Workspace.class)));
            var me = mock(WorkspaceMember.class);
            when(me.getId()).thenReturn(11L);
            when(currentUser.getCurrentMember()).thenReturn(me);
            var mine = mock(TaskDefinition.class);
            var another = mock(TaskDefinition.class);
            var foreignAuthor = mock(WorkspaceMember.class);
            when(foreignAuthor.getId()).thenReturn(22L);
            when(mine.getCreatedBy()).thenReturn(me);
            when(another.getCreatedBy()).thenReturn(foreignAuthor);
            when(definitions.findByWorkspaceIdOrderByIdAsc(5L)).thenReturn(List.of(mine, another));

            assertEquals(List.of(mine), tasks.getDefinitionsByParticipation(5L, "created"));
        }
    }

    @Test
    void adminViewContainsAuthorOrScopedManagerWithoutExplicitAdminParticipation() throws Exception {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            var workspace = mock(Workspace.class);
            when(workspaces.findById(5L)).thenReturn(java.util.Optional.of(workspace));
            var member = mock(WorkspaceMember.class);
            when(currentUser.getCurrentMember()).thenReturn(member);
            var manageable = mock(TaskDefinition.class);
            var unrelated = mock(TaskDefinition.class);
            when(definitions.findByWorkspaceIdOrderByIdAsc(5L)).thenReturn(List.of(manageable, unrelated));
            when(authorization.canManage(manageable)).thenReturn(true);

            assertEquals(List.of(manageable), tasks.getDefinitionsByParticipation(5L, "admin"));
            verify(authorization).canManage(unrelated);
        }
    }
}
