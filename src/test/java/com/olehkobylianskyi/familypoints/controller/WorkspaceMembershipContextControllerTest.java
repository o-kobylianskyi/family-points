package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceMembershipContextControllerTest {
    @Test
    void returnsRoleFromSelectedWorkspaceNotLegacyAccount() {
        var current = mock(CurrentUserService.class);
        var resolver = mock(WorkspaceMembershipResolver.class);
        var account = mock(UserAccount.class);
        var member = mock(WorkspaceMember.class);
        var workspace = mock(Workspace.class);
        var role = mock(WorkspaceRole.class);
        when(current.getCurrentAccount()).thenReturn(account);
        when(resolver.requireActiveMember(account, 22L)).thenReturn(member);
        when(member.getWorkspace()).thenReturn(workspace);
        when(member.getWorkspaceRole()).thenReturn(role);
        when(member.getId()).thenReturn(44L);
        when(member.getName()).thenReturn("Second member");
        when(workspace.getId()).thenReturn(22L);
        when(workspace.getName()).thenReturn("Second family");
        when(role.getWorkspace()).thenReturn(workspace);
        when(role.getCode()).thenReturn("CHILD");
        when(role.getPermissions()).thenReturn(Set.of());

        var result = new WorkspaceMembershipContextController(current, resolver).context(22L);
        assertEquals(22L, result.workspaceId());
        assertEquals(44L, result.memberId());
        assertEquals("CHILD", result.roleCode());
        assertTrue(result.permissions().isEmpty());
        verify(resolver).requireActiveMember(account, 22L);
    }

    @Test
    void rejectsMembershipWithForeignRole() {
        var current = mock(CurrentUserService.class);
        var resolver = mock(WorkspaceMembershipResolver.class);
        var account = mock(UserAccount.class);
        var member = mock(WorkspaceMember.class);
        var role = mock(WorkspaceRole.class);
        var otherWorkspace = mock(Workspace.class);
        when(current.getCurrentAccount()).thenReturn(account);
        when(resolver.requireActiveMember(account, 22L)).thenReturn(member);
        when(member.getWorkspaceRole()).thenReturn(role);
        when(role.getWorkspace()).thenReturn(otherWorkspace);
        when(otherWorkspace.getId()).thenReturn(33L);
        var controller = new WorkspaceMembershipContextController(current, resolver);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> controller.context(22L)).getStatusCode().value());
    }
}
