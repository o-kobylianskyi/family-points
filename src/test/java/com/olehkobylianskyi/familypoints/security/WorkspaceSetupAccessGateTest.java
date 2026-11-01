package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real production workspace-security predicate used by all Issue18 manager-only routes. */
class WorkspaceSetupAccessGateTest {
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final WorkspaceSecurity security = new WorkspaceSecurity(current);
    private final WorkspaceMember member = mock(WorkspaceMember.class);

    @Test void deniesChildEvenIfMemberBelongsToSelectedWorkspace() {
        when(current.belongsToWorkspace(10L)).thenReturn(true);
        when(current.getCurrentMember()).thenReturn(member);
        when(member.hasPermission(WorkspacePermission.MANAGE_WORKSPACE)).thenReturn(false);
        when(member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)).thenReturn(false);
        assertFalse(security.canManageWorkspace(10L));
        verify(member).hasPermission(WorkspacePermission.MANAGE_WORKSPACE);
        verify(member).hasPermission(WorkspacePermission.ADMIN_OVERRIDE);
    }

    @Test void deniesForeignFamilyBeforeReadingMemberPermissions() {
        when(current.belongsToWorkspace(77L)).thenReturn(false);
        assertFalse(security.canManageWorkspace(77L));
        verify(current,never()).getCurrentMember();
        verifyNoInteractions(member);
    }

    @Test void permitsSelectedManagerAndAdminOverrideOnlyInTheirFamily() {
        when(current.belongsToWorkspace(10L)).thenReturn(true);
        when(current.getCurrentMember()).thenReturn(member);
        when(member.hasPermission(WorkspacePermission.MANAGE_WORKSPACE)).thenReturn(true);
        assertTrue(security.canManageWorkspace(10L));
        when(member.hasPermission(WorkspacePermission.MANAGE_WORKSPACE)).thenReturn(false);
        when(member.hasPermission(WorkspacePermission.ADMIN_OVERRIDE)).thenReturn(true);
        assertTrue(security.canManageWorkspace(10L));
        when(current.belongsToWorkspace(77L)).thenReturn(false);
        assertFalse(security.canManageWorkspace(77L));
    }

    @Test void unrelatedPermissionsNeverGrantWorkspaceManagement() {
        when(current.belongsToWorkspace(10L)).thenReturn(true);
        when(current.getCurrentMember()).thenReturn(member);
        when(member.hasPermission(WorkspacePermission.MANAGE_POINTS)).thenReturn(true);
        when(member.hasPermission(WorkspacePermission.MANAGE_REWARDS)).thenReturn(true);
        assertFalse(security.canManageWorkspace(10L));
    }
}
