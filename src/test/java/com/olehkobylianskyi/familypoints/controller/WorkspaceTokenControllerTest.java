package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.LoginResponse;
import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceTokenControllerTest {
    @Test
    void allowsOnlyVerifiedMembershipAndReturnsWorkspaceToken() {
        var current = mock(CurrentUserService.class);
        var sessions = mock(WorkspaceSessionService.class);
        var jwt = mock(JwtService.class);
        var account = mock(UserAccount.class);
        when(current.getCurrentAccount()).thenReturn(account);
        when(jwt.generateWorkspaceAccessToken(any(), eq(12L))).thenReturn("token-for-12");
        when(jwt.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        var principal = User.withUsername("member").password("unused").roles("USER").build();
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        var controller = new WorkspaceTokenController(current, sessions, jwt);

        LoginResponse response = controller.activate(12L, authentication).getBody();
        assertNotNull(response);
        assertEquals("token-for-12", response.getAccessToken());
        verify(sessions).requireSession(account, 12L);
        verify(jwt).generateWorkspaceAccessToken(principal, 12L);
    }

    @Test
    void rejectedMembershipNeverIssuesToken() {
        var current = mock(CurrentUserService.class);
        var sessions = mock(WorkspaceSessionService.class);
        var jwt = mock(JwtService.class);
        var account = mock(UserAccount.class);
        when(current.getCurrentAccount()).thenReturn(account);
        when(sessions.requireSession(account, 42L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        var principal = User.withUsername("member").password("unused").roles("USER").build();
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        var controller = new WorkspaceTokenController(current, sessions, jwt);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> controller.activate(42L, authentication)).getStatusCode().value());
        verifyNoInteractions(jwt);
    }
}
