package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.service.WorkspaceMemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import java.sql.ResultSet;
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
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final WorkspaceMemberController controller =
            new WorkspaceMemberController(members, memberships, accounts, db);

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
        assertNull(result.get(0).getAccessStatus());
        assertNull(result.get(0).getInvitationEmail());
        verifyNoInteractions(memberships, accounts, db);
    }

    @Test
    void managerGetsEmailFromScopedMembership() {
        var member = member();
        as("MANAGE_MEMBERS");
        var account = mock(UserAccount.class);
        when(account.getEmail()).thenReturn("child@example.org");
        when(account.isEnabled()).thenReturn(true);
        var linked = mock(AccountWorkspaceMembership.class);
        when(linked.getWorkspaceMember()).thenReturn(member);
        when(linked.getAccount()).thenReturn(account);
        when(memberships.findAllByWorkspaceId(5L)).thenReturn(List.of(linked));
        when(accounts.findAllByWorkspaceMemberWorkspaceIdOrderByWorkspaceMemberIdAsc(5L))
                .thenReturn(List.of());
        var response = controller.getMembers(5L).get(0);
        assertEquals("child@example.org", response.getAccountEmail());
        assertEquals("ACTIVE", response.getAccessStatus());
        assertNull(response.getInvitationEmail());
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
        var response = controller.getMembers(5L).get(0);
        assertEquals("legacy@example.org", response.getAccountEmail());
        assertEquals("DISABLED", response.getAccessStatus());
    }
    @Test
    void managerSeesPendingInvitationNotAsAcceptedAccountEmail() throws Exception {
        member();
        as("MANAGE_MEMBERS");
        org.mockito.Mockito.doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet row = mock(ResultSet.class);
            when(row.getLong("member_id")).thenReturn(7L);
            when(row.getString("invited_email")).thenReturn("pending@example.org");
            handler.processRow(row);
            return null;
        }).when(db).query(anyString(), any(RowCallbackHandler.class), eq(5L));

        var response = controller.getMembers(5L).get(0);
        assertEquals("PENDING_INVITATION", response.getAccessStatus());
        assertEquals("pending@example.org", response.getInvitationEmail());
        assertNull(response.getAccountEmail());
    }

    @Test
    void managerSeesNoAccountWhenNothingLinkedOrPending() {
        member();
        as("MANAGE_MEMBERS");
        var response = controller.getMembers(5L).get(0);
        assertEquals("NO_ACCOUNT", response.getAccessStatus());
        assertNull(response.getAccountEmail());
        assertNull(response.getInvitationEmail());
    }

}
