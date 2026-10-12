package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceSessionServiceTest {
    private final WorkspaceContextAuthorization authorization = mock(WorkspaceContextAuthorization.class);
    private final WorkspaceSessionService service = new WorkspaceSessionService(authorization);

    @Test
    void selectedWorkspaceSessionUsesOnlySelectedMemberRolePermissions() {
        UserAccount account = mock(UserAccount.class);
        WorkspaceMember member = mock(WorkspaceMember.class);
        WorkspaceRole role = mock(WorkspaceRole.class);
        when(account.getId()).thenReturn(1L);
        when(account.getUsername()).thenReturn("same-user");
        when(member.getId()).thenReturn(22L);
        when(member.getWorkspaceRole()).thenReturn(role);
        when(role.getPermissions()).thenReturn(Set.of(WorkspacePermission.MANAGE_POINTS));
        when(authorization.requireActiveContext(account, 2L)).thenReturn(member);

        var session = service.requireSession(account, 2L);
        assertEquals(1L, session.accountId());
        assertEquals(2L, session.workspaceId());
        assertEquals(22L, session.memberId());
        assertTrue(session.hasPermission(WorkspacePermission.MANAGE_POINTS));
        assertFalse(session.hasPermission(WorkspacePermission.MANAGE_MEMBERS));
        verify(authorization).requireActiveContext(account, 2L);
    }

    @Test
    void rejectsUnauthorizedWorkspaceInsteadOfFallingBackToPrimaryMember() {
        UserAccount account = mock(UserAccount.class);
        when(authorization.requireActiveContext(account, 999L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.requireSession(account, 999L)).getStatusCode().value());
    }

    @Test
    void immutablePermissionsSnapshotCannotBeExtended() {
        var session = new WorkspaceSessionService.WorkspaceSession(
                1L, "user", 2L, 3L, Set.of(WorkspacePermission.MANAGE_POINTS));
        assertThrows(UnsupportedOperationException.class,
                () -> session.permissions().add(WorkspacePermission.MANAGE_MEMBERS));
        assertFalse(session.hasPermission(null));
    }
}
