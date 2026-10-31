package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceContextAuthorizationTest {
    private final WorkspaceMembershipResolver resolver = mock(WorkspaceMembershipResolver.class);
    private final WorkspaceContextAuthorization authorization = new WorkspaceContextAuthorization(resolver);

    @Test
    void usesRoleFromSelectedMembershipNotThePrimaryAccount() {
        UserAccount account = mock(UserAccount.class);
        WorkspaceMember selected = mock(WorkspaceMember.class);
        WorkspaceRole selectedRole = mock(WorkspaceRole.class);
        Workspace selectedWorkspace = mock(Workspace.class);
        when(resolver.requireActiveMember(account, 42L)).thenReturn(selected);
        when(selected.getWorkspaceRole()).thenReturn(selectedRole);
        when(selectedRole.getWorkspace()).thenReturn(selectedWorkspace);
        when(selectedWorkspace.getId()).thenReturn(42L);
        when(selected.hasPermission(WorkspacePermission.MANAGE_POINTS)).thenReturn(true);

        assertTrue(authorization.hasPermission(account, 42L, WorkspacePermission.MANAGE_POINTS));
        assertFalse(authorization.hasPermission(account, 42L, WorkspacePermission.MANAGE_MEMBERS));
    }

    @Test
    void deniesForeignWorkspaceRoleEvenIfMemberClaimsAdminPermission() {
        UserAccount account = mock(UserAccount.class);
        WorkspaceMember member = mock(WorkspaceMember.class);
        WorkspaceRole role = mock(WorkspaceRole.class);
        Workspace anotherWorkspace = mock(Workspace.class);
        when(resolver.requireActiveMember(account, 42L)).thenReturn(member);
        when(member.getWorkspaceRole()).thenReturn(role);
        when(role.getWorkspace()).thenReturn(anotherWorkspace);
        when(anotherWorkspace.getId()).thenReturn(77L);
        when(member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)).thenReturn(true);

        assertFalse(authorization.hasPermission(account, 42L, WorkspacePermission.MANAGE_POINTS));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> authorization.requireActiveContext(account, 42L)).getStatusCode().value());
    }

    @Test
    void deniesMissingMembershipAndNullPermissions() {
        UserAccount account = mock(UserAccount.class);
        when(resolver.requireActiveMember(account, 42L)).thenThrow(
                new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        assertFalse(authorization.hasPermission(account, 42L, WorkspacePermission.MANAGE_POINTS));
        assertFalse(authorization.hasPermission(account, 42L, null));
    }
}
