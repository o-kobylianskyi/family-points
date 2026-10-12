package com.olehkobylianskyi.familypoints.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defense-in-depth for every family-scoped API route. Endpoint-specific role
 * checks are still required; this filter prevents a JWT for family A from
 * addressing family B, including controllers that lack method security.
 *
 * This filter is instantiated inside the Spring Security chain (not a servlet
 * @Component), after JWT authentication. Otherwise Boot could register it
 * before authentication and the OncePerRequestFilter guard would skip it.
 */
public final class WorkspaceIsolationFilter extends OncePerRequestFilter {

    private static final Pattern WORKSPACE_ROUTE =
            Pattern.compile("^/workspaces/([0-9]+)(?:/|$)");

    private final CurrentUserService currentUser;

    public WorkspaceIsolationFilter(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        Matcher route = WORKSPACE_ROUTE.matcher(request.getServletPath());
        if (route.find()) {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            // Authentication itself is handled by Spring Security.
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                boolean allowed;
                try {
                    allowed = currentUser.belongsToWorkspace(Long.parseLong(route.group(1)));
                } catch (NumberFormatException | IllegalStateException exception) {
                    allowed = false;
                }
                if (!allowed) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Workspace access denied");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }
}
