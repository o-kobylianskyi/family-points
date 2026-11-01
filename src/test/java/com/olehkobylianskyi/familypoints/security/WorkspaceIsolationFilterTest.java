package com.olehkobylianskyi.familypoints.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceIsolationFilterTest {
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final WorkspaceIsolationFilter filter = new WorkspaceIsolationFilter(currentUser);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("family-user", "N/A", List.of())
        );
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    @Test
    void rejectsAnotherFamilyBeforeItReachesController() throws Exception {
        authenticate();
        when(currentUser.belongsToWorkspace(200L)).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("/workspaces/200/members/12/points/history"), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest(), "The controller must not be reached");
        verify(currentUser).belongsToWorkspace(200L);
    }

    @Test
    void allowsTheAuthenticatedFamily() throws Exception {
        authenticate();
        when(currentUser.belongsToWorkspace(100L)).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("/workspaces/100/rewards"), response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        verify(currentUser).belongsToWorkspace(100L);
    }

    @Test
    void letsUnauthenticatedRequestProceedToSpringSecurity() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("/workspaces/100/rewards"), response, chain);

        assertNotNull(chain.getRequest());
        verifyNoInteractions(currentUser);
    }

    @Test
    void skipsGlobalAuthenticationAndHealthEndpoints() throws Exception {
        authenticate();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/auth/login"), new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
        verifyNoInteractions(currentUser);
    }
    @Test
    void deniesRevokedSelectedWorkspaceBeforeController() throws Exception {
        authenticate();
        when(currentUser.belongsToWorkspace(200L)).thenThrow(
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Membership revoked"));
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request("/workspaces/200/tasks"), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void deniesDifferentWorkspaceEvenWithAuthenticatedScopedSession() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken("family-user", "N/A", List.of());
        authentication.setDetails(new WorkspaceAuthenticationDetails(5L, 100L, 7L));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(currentUser.belongsToWorkspace(200L)).thenReturn(false);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request("/workspaces/200/rewards"), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void setupReviewFinalizeAndReceiptsBlockForeignWorkspaceBeforeControllers() throws Exception {
        authenticate();
        when(currentUser.belongsToWorkspace(200L)).thenReturn(false);
        for (String path : List.of(
                "/workspaces/200/setup",
                "/workspaces/200/setup/review",
                "/workspaces/200/setup/finalize-import",
                "/workspaces/200/setup/finalize-import/receipts/00000000-0000-4000-8000-000000000001",
                "/workspaces/200/templates/imports/idempotent",
                "/workspaces/200/templates/imports/receipts/00000000-0000-4000-8000-000000000001",
                "/workspaces/200/templates/42/activate-task/idempotent",
                "/workspaces/200/templates/42/activate-reward/idempotent",
                "/workspaces/200/templates/activations/receipts/00000000-0000-4000-8000-000000000001")) {
            var response=new MockHttpServletResponse();
            var chain=new MockFilterChain();
            filter.doFilter(request(path),response,chain);
            assertEquals(403,response.getStatus(),path);
            assertNull(chain.getRequest(),"Must not invoke any onboarding controller for "+path);
        }
        verify(currentUser,times(9)).belongsToWorkspace(200L);
    }

    @Test
    void setupRoutesAllowSelectedMembershipPastIsolationFilterOnly() throws Exception {
        authenticate();
        when(currentUser.belongsToWorkspace(100L)).thenReturn(true);
        for(String path : List.of("/workspaces/100/setup/review",
                "/workspaces/100/setup/finalize-import",
                "/workspaces/100/templates/10/activate-task/idempotent",
                "/workspaces/100/templates/activations/receipts/00000000-0000-4000-8000-000000000001")) {
            var response=new MockHttpServletResponse();
            var chain=new MockFilterChain();
            filter.doFilter(request(path),response,chain);
            assertEquals(200,response.getStatus());
            assertNotNull(chain.getRequest(),path);
        }
    }

}
