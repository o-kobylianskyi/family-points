package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceMembershipResolverTest {
    private final AccountWorkspaceMembershipRepository repository = mock(AccountWorkspaceMembershipRepository.class);
    private final WorkspaceMembershipResolver resolver = new WorkspaceMembershipResolver(repository);

    @Test
    void permitsOnlyMatchingActiveMembership() {
        UserAccount account = account(10L, true);
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(20L);
        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.isActive()).thenReturn(true);
        when(member.getWorkspace()).thenReturn(workspace);
        AccountWorkspaceMembership link = mock(AccountWorkspaceMembership.class);
        when(link.getWorkspaceMember()).thenReturn(member);
        when(repository.findByAccountIdAndWorkspaceMemberWorkspaceId(10L, 20L))
                .thenReturn(Optional.of(link));

        assertSame(member, resolver.requireActiveMember(account, 20L));
    }

    @Test
    void deniesMembershipNotOwnedByAccount() {
        UserAccount account = account(10L, true);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> resolver.requireActiveMember(account, 999L)).getStatusCode().value());
    }

    @Test
    void deniesInactiveMemberAndDisabledAccount() {
        UserAccount account = account(10L, true);
        WorkspaceMember member = mock(WorkspaceMember.class);
        AccountWorkspaceMembership link = mock(AccountWorkspaceMembership.class);
        when(link.getWorkspaceMember()).thenReturn(member);
        when(repository.findByAccountIdAndWorkspaceMemberWorkspaceId(10L, 20L))
                .thenReturn(Optional.of(link));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> resolver.requireActiveMember(account, 20L)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> resolver.requireActiveMember(account(11L, false), 20L)).getStatusCode().value());
    }

    @Test
    void deniesForgedWorkspaceReferenceAndNullId() {
        UserAccount account = account(10L, true);
        Workspace workspace = mock(Workspace.class);
        when(workspace.getId()).thenReturn(99L);
        WorkspaceMember member = mock(WorkspaceMember.class);
        when(member.isActive()).thenReturn(true);
        when(member.getWorkspace()).thenReturn(workspace);
        AccountWorkspaceMembership link = mock(AccountWorkspaceMembership.class);
        when(link.getWorkspaceMember()).thenReturn(member);
        when(repository.findByAccountIdAndWorkspaceMemberWorkspaceId(10L, 20L))
                .thenReturn(Optional.of(link));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> resolver.requireActiveMember(account, 20L)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> resolver.requireActiveMember(account, null)).getStatusCode().value());
    }

    private UserAccount account(Long id, boolean enabled) {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(id);
        when(account.isEnabled()).thenReturn(enabled);
        return account;
    }
}
