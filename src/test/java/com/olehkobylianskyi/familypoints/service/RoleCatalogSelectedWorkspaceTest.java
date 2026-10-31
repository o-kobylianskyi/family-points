package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleCatalogSelectedWorkspaceTest {
    @Test void defaultAccountRoleCannotGrantPermissionInAnotherSelectedFamily() {
        var current = mock(CurrentUserService.class);
        var selected = mock(WorkspaceMember.class);
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        when(current.getCurrentMember()).thenReturn(selected);
        var authorization = new RoleCatalogAuthorizationService(current);
        assertThrows(AccessDeniedException.class, () -> authorization.requireManage(7L));
        verify(current, never()).getCurrentAccount();
    }

    @Test void selectedFamilyRoleGrantsItsOwnPermissionWithoutLegacyAccountRole() {
        var current = mock(CurrentUserService.class);
        var selected = mock(WorkspaceMember.class);
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        when(current.getCurrentMember()).thenReturn(selected);
        when(selected.hasPermission(WorkspacePermission.MANAGE_ROLES)).thenReturn(true);
        assertDoesNotThrow(() -> new RoleCatalogAuthorizationService(current).requireManage(7L));
        verify(current, never()).getCurrentAccount();
    }

    @Test void wrongWorkspaceIsDeniedBeforeReadingRole() {
        var current = mock(CurrentUserService.class);
        assertThrows(AccessDeniedException.class, () -> new RoleCatalogAuthorizationService(current).requireManage(7L));
        verify(current, never()).getCurrentMember();
    }
}
