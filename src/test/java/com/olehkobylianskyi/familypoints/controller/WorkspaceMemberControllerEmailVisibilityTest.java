package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.service.WorkspaceMemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceMemberControllerEmailVisibilityTest {
    private final WorkspaceMemberService members = mock(WorkspaceMemberService.class);
    private final AccountWorkspaceMembershipRepository memberships = mock(AccountWorkspaceMembershipRepository.class);
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final WorkspaceMemberController controller =
            new WorkspaceMemberController(members, memberships, accounts);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void as(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
    }

    private WorkspaceMember member() {
        var member = mock(WorkspaceMember.class);
        var workspace = mock(Workspace.class);
        var role = mock(WorkspaceRole.class);
        when(member.getId()).thenReturn(7L);
        when(member.getName()).thenReturn("Test2");
        when(member.getMemberType()).thenReturn(WorkspaceMemberType.CHILD);
        when(member.getWorkspaceRole()).thenReturn(role);
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(5L);
        when(role.getId()).thenReturn(3L);
        when(role.getCode()).thenReturn("CHILD");
        when(role.getName()).thenReturn("Дитина");
        when(members.getMembers(5L)).thenReturn(List.of(member));
        return member;
    }

    @Test
    void memberViewerCannotReadAccountEmailOrTriggerEmailLookup() {
        member();
        as("VIEW_WORKSPACE");
        var result = controller.getMembers(5L);
        assertEquals(1, result.size());
        assertNull(result.get(0).getAccountEmail());
        verifyNoInteractions(memberships, accounts);
    }

    @Test
    void managerGetsEmailFromScopedMembership() {
        var member = member();
        as("MANAGE_MEMBERS");
        var account = mock(UserAccount.class);
        when(account.getEmail()).thenReturn("child@example.org");
        var linked = mock(AccountWorkspaceMembership.class);
        when(linked.getWorkspaceMember()).thenReturn(member);
        when(linked.getAccount()).thenReturn(account);
        when(memberships.findAllByWorkspaceId(5L)).thenReturn(List.of(linked));
        when(accounts.findAllByWorkspaceMemberWorkspaceIdOrderByWorkspaceMemberIdAsc(5L))
                .thenReturn(List.of());
        assertEquals("child@example.org", controller.getMembers(5L).get(0).getAccountEmail());
        verify(memberships).findAllByWorkspaceId(5L);
    }

    @Test
    void managerFallsBackToLegacyMemberAccount() {
        var member = member();
        as("MANAGE_MEMBERS");
        var account = mock(UserAccount.class);
        when(account.getEmail()).thenReturn("legacy@example.org");
        when(account.getWorkspaceMember()).thenReturn(member);
        when(memberships.findAllByWorkspaceId(5L)).thenReturn(List.of());
        when(accounts.findAllByWorkspaceMemberWorkspaceIdOrderByWorkspaceMemberIdAsc(5L))
                .thenReturn(List.of(account));
        assertEquals("legacy@example.org", controller.getMembers(5L).get(0).getAccountEmail());
    }
}
