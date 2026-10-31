package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.entity.WorkspaceRole;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserResponseAccountIdentityTest {
    @Test
    void identityIsAccountIdRatherThanUsernameOrWorkspaceMemberId() throws Exception {
        var accountA = mock(UserAccount.class);
        var accountB = mock(UserAccount.class);
        var member = mock(WorkspaceMember.class);
        var role = mock(WorkspaceRole.class);
        var workspace = mock(Workspace.class);
        when(accountA.getId()).thenReturn(100L);
        when(accountB.getId()).thenReturn(200L);
        when(accountA.getUsername()).thenReturn("same-member-account-A");
        when(accountB.getUsername()).thenReturn("same-member-account-B");
        when(member.getId()).thenReturn(17L);
        when(member.getName()).thenReturn("Shared family member");
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(2L);
        when(member.getWorkspaceRole()).thenReturn(role);
        when(role.getPermissions()).thenReturn(java.util.Set.of());
        when(role.getId()).thenReturn(9L);
        when(role.getCode()).thenReturn("PARENT");
        when(role.getName()).thenReturn("Parent");
        when(member.getMemberType()).thenReturn(com.olehkobylianskyi.familypoints.entity.WorkspaceMemberType.PARENT);
        var a = CurrentUserResponse.from(accountA, member);
        var b = CurrentUserResponse.from(accountB, member);
        assertEquals(100L, a.getAccountId());
        assertEquals(200L, b.getAccountId());
        assertEquals(a.getMemberId(), b.getMemberId());
        assertEquals(a.getWorkspaceId(), b.getWorkspaceId());
        assertNotEquals(a.getAccountId(), b.getAccountId());
    }
}
