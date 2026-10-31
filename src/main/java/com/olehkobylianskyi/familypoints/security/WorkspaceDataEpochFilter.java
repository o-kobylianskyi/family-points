package com.olehkobylianskyi.familypoints.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generation barrier for workspace mutations. Legacy clients are accepted only
 * until the first reset (epoch 0). After reset, every workspace write must carry
 * the current epoch. Actual reset execution must lock family and increment the
 * generation in one transaction; that atomicity is NOT implemented here.
 */
public final class WorkspaceDataEpochFilter extends OncePerRequestFilter {
    private static final Pattern ROUTE=Pattern.compile("^/workspaces/([0-9]+)(?:/|$)");
    public static final String HEADER="X-Workspace-Data-Epoch";
    private final JdbcTemplate db;

    public WorkspaceDataEpochFilter(JdbcTemplate db) { this.db=db; }

    @Override protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!isMutation(request.getMethod())) {
            chain.doFilter(request,response);
            return;
        }
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request,response);
            return;
        }
        Matcher match=ROUTE.matcher(request.getServletPath());
        if (!match.find()) {
            chain.doFilter(request,response);
            return;
        }
        long workspace=Long.parseLong(match.group(1));
        // The filter runs after JWT authentication and workspace isolation.
        Long current=db.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?",Long.class,workspace);
        if (current==null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,"Workspace not found");
            return;
        }
        String header=request.getHeader(HEADER);
        if (header==null && current==0) {
            chain.doFilter(request,response);
            return;
        }
        long provided;
        try {
            if (header==null) throw new NumberFormatException("missing epoch");
            provided=Long.parseLong(header);
            if (provided<0) throw new NumberFormatException("negative epoch");
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_CONFLICT,"WORKSPACE_DATA_EPOCH_REQUIRED");
            return;
        }
        if (provided!=current) {
            response.sendError(HttpServletResponse.SC_CONFLICT,"WORKSPACE_DATA_EPOCH_STALE");
            return;
        }
        chain.doFilter(request,response);
    }

    private static boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
