package com.olehkobylianskyi.familypoints.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterWorkspaceScopeTest {
    private final JwtService jwt = new JwtService(
            Base64.getEncoder().encodeToString(
                    "family-points-filter-test-secret-32-bytes-minimum".getBytes(StandardCharsets.UTF_8)),
            3_600_000L);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void legacyTokenStillAuthenticates() throws Exception {
        var user = User.withUsername("tester").password("unused").roles("USER").build();
        var details = mock(UserDetailsService.class);
        when(details.loadUserByUsername("tester")).thenReturn(user);
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt.generateAccessToken(user));

        new JwtAuthenticationFilter(jwt, details).doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void workspaceScopedTokenIsNotAuthenticatedByLegacyFilter() throws Exception {
        var user = User.withUsername("tester").password("unused").roles("USER").build();
        var details = mock(UserDetailsService.class);
        when(details.loadUserByUsername("tester")).thenReturn(user);
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer "
                + jwt.generateWorkspaceAccessToken(user, 27L));

        new JwtAuthenticationFilter(jwt, details).doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
    @Test
    void selectedWorkspaceUsesItsOwnPermissions() throws Exception {
        var user = User.withUsername("tester").password("unused")
                .authorities("ADMIN_OVERRIDE").build();
        var details = mock(UserDetailsService.class);
        var accounts = mock(UserAccountRepository.class);
        var sessions = mock(WorkspaceSessionService.class);
        var account = mock(UserAccount.class);
        when(details.loadUserByUsername("tester")).thenReturn(user);
        when(accounts.findWithSecurityDataByUsername("tester")).thenReturn(Optional.of(account));
        when(sessions.requireSession(account, 27L)).thenReturn(
                new WorkspaceSessionService.WorkspaceSession(
                        5L, "tester", 27L, 9L, Set.of(WorkspacePermission.MANAGE_POINTS)));
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer "
                + jwt.generateWorkspaceAccessToken(user, 27L));

        new JwtAuthenticationFilter(jwt, details, accounts, sessions).doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(27L, ((WorkspaceAuthenticationDetails) authentication.getDetails()).workspaceId());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MANAGE_POINTS")));
        assertFalse(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN_OVERRIDE")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void selectedWorkspaceWithoutMembershipCannotAuthenticate() throws Exception {
        var user = User.withUsername("tester").password("unused").roles("USER").build();
        var details = mock(UserDetailsService.class);
        var accounts = mock(UserAccountRepository.class);
        var sessions = mock(WorkspaceSessionService.class);
        var account = mock(UserAccount.class);
        when(details.loadUserByUsername("tester")).thenReturn(user);
        when(accounts.findWithSecurityDataByUsername("tester")).thenReturn(Optional.of(account));
        when(sessions.requireSession(account, 999L)).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN));
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer "
                + jwt.generateWorkspaceAccessToken(user, 999L));

        new JwtAuthenticationFilter(jwt, details, accounts, sessions).doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }


    @Test
    void activationRouteWithoutAuthorizationLeavesAuthenticationToReturn401() throws Exception {
        var details=mock(UserDetailsService.class);
        var request=mock(HttpServletRequest.class);
        var response=mock(HttpServletResponse.class);
        var chain=mock(FilterChain.class);
        when(request.getServletPath()).thenReturn(
                "/workspaces/27/templates/99/activate-task/idempotent");
        new JwtAuthenticationFilter(jwt,details).doFilter(request,response,chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(details);
        verify(chain).doFilter(request,response);
        // SecurityConfig.anyRequest().authenticated() subsequently answers 401;
        // the fingerprint serializer is never reached by this filter alone.
    }

    @Test
    void activationRouteScopedJwtDoesNotBorrowCreateTasksPermissionFromAnotherFamily() throws Exception {
        var user=User.withUsername("tester").password("unused")
                .authorities("CREATE_TASKS","ADMIN_OVERRIDE").build();
        var details=mock(UserDetailsService.class);
        var accounts=mock(UserAccountRepository.class);
        var sessions=mock(WorkspaceSessionService.class);
        var account=mock(UserAccount.class);
        when(details.loadUserByUsername("tester")).thenReturn(user);
        when(accounts.findWithSecurityDataByUsername("tester")).thenReturn(Optional.of(account));
        when(sessions.requireSession(account,27L)).thenReturn(
                new WorkspaceSessionService.WorkspaceSession(
                        5L,"tester",27L,9L,Set.of(WorkspacePermission.MANAGE_POINTS)));
        var request=mock(HttpServletRequest.class);
        var response=mock(HttpServletResponse.class);
        var chain=mock(FilterChain.class);
        when(request.getServletPath()).thenReturn(
                "/workspaces/27/templates/99/activate-task/idempotent");
        when(request.getHeader("Authorization")).thenReturn("Bearer "+
                jwt.generateWorkspaceAccessToken(user,27L));
        new JwtAuthenticationFilter(jwt,details,accounts,sessions).doFilter(
                request,response,chain);
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(27L,((WorkspaceAuthenticationDetails)authentication.getDetails()).workspaceId());
        assertTrue(authentication.getAuthorities().stream().noneMatch(
                authority->authority.getAuthority().equals("CREATE_TASKS")));
        assertTrue(authentication.getAuthorities().stream().noneMatch(
                authority->authority.getAuthority().equals("ADMIN_OVERRIDE")));
        verify(chain).doFilter(request,response);
        // TemplateActivationReceiptController method authorization requires
        // CREATE_TASKS, therefore scoped authenticated denial is not a LocalDate 500.
    }
}
