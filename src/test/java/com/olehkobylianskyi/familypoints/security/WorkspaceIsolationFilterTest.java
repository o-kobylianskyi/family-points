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
}
