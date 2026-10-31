package com.olehkobylianskyi.familypoints.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserAccountRepository accounts;
    private final WorkspaceSessionService workspaceSessions;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                   UserAccountRepository accounts, WorkspaceSessionService workspaceSessions) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.accounts = accounts;
        this.workspaceSessions = workspaceSessions;
    }

    // Kept only for existing isolated legacy-filter unit tests.
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this(jwtService, userDetailsService, null, null);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7);

        try {

            String username =
                    jwtService.extractUsername(token);

            if (username != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    Long selectedWorkspaceId = jwtService.extractWorkspaceId(token);
                    UsernamePasswordAuthenticationToken authentication;

                    if (selectedWorkspaceId == null) {
                        authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    } else {
                        // The signed claim identifies a requested workspace, not authorization.
                        // Recheck membership on every request and use ONLY that workspace's rights.
                        if (accounts == null || workspaceSessions == null) {
                            throw new IllegalStateException("Scoped JWT authentication not configured");
                        }
                        var account = accounts.findWithSecurityDataByUsername(username)
                                .orElseThrow(() -> new IllegalStateException("Account not found"));
                        var session = workspaceSessions.requireSession(account, selectedWorkspaceId);
                        var authorities = session.permissions().stream()
                                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                                .toList();
                        authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, authorities);
                        authentication.setDetails(new WorkspaceAuthenticationDetails(
                                session.accountId(), session.workspaceId(), session.memberId()));
                    }

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception exception) {
            log.warn(
                    "JWT rejected for {} {}: {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }

        filterChain.doFilter(request, response);
    }
}