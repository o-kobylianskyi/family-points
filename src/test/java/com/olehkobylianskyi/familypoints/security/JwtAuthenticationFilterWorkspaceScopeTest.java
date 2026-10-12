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
}
