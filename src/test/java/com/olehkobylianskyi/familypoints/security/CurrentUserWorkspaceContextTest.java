package com.olehkobylianskyi.familypoints.security;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserWorkspaceContextTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final WorkspaceMembershipResolver resolver = mock(WorkspaceMembershipResolver.class);
    private final CurrentUserService current = new CurrentUserService(accounts, resolver);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private UserAccount signIn(Object details) {
        var account = mock(UserAccount.class);
        when(account.getId()).thenReturn(5L);
        when(accounts.findWithSecurityDataByUsername("tester")).thenReturn(Optional.of(account));
        var auth = new UsernamePasswordAuthenticationToken("tester", null, List.of());
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);
        return account;
    }

    @Test
    void scopedContextUsesVerifiedWorkspaceMember() {
        var account = signIn(new WorkspaceAuthenticationDetails(5L, 99L, 11L));
        var member = mock(WorkspaceMember.class);
        when(member.getId()).thenReturn(11L);
        when(resolver.requireActiveMember(account, 99L)).thenReturn(member);

        assertSame(member, current.getCurrentMember());
        verify(resolver).requireActiveMember(account, 99L);
    }

    @Test
    void rejectsMismatchedAccountAndMemberInContext() {
        signIn(new WorkspaceAuthenticationDetails(999L, 99L, 11L));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                current::getCurrentMember).getStatusCode().value());

        SecurityContextHolder.clearContext();
        var account = signIn(new WorkspaceAuthenticationDetails(5L, 99L, 11L));
        var member = mock(WorkspaceMember.class);
        when(member.getId()).thenReturn(12L);
        when(resolver.requireActiveMember(account, 99L)).thenReturn(member);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                current::getCurrentMember).getStatusCode().value());
    }

    @Test
    void legacyAuthenticationStillUsesPrimaryMember() {
        var account = signIn(null);
        var member = mock(WorkspaceMember.class);
        when(account.getWorkspaceMember()).thenReturn(member);
        assertSame(member, current.getCurrentMember());
        verifyNoInteractions(resolver);
    }
}
