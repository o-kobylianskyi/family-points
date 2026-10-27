package com.olehkobylianskyi.familypoints.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceDataEpochFilterTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final FilterChain chain=mock(FilterChain.class);
    private final WorkspaceDataEpochFilter filter=new WorkspaceDataEpochFilter(db);

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private MockHttpServletResponse call(String method,String header,long epoch) throws Exception {
        var authentication=new TestingAuthenticationToken("member","none","ROLE_USER");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,7L))
                .thenReturn(epoch);
        var request=new MockHttpServletRequest(method,"/workspaces/7/tasks");
        request.setServletPath("/workspaces/7/tasks");
        if(header!=null) request.addHeader(WorkspaceDataEpochFilter.HEADER,header);
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,chain);
        return response;
    }

    @Test void oldClientAllowedBeforeResetButRejectedAfterReset() throws Exception {
        assertEquals(200,call("POST",null,0).getStatus());
        verify(chain).doFilter(any(),any());
        clearInvocations(chain);
        assertEquals(409,call("POST",null,1).getStatus());
        verifyNoInteractions(chain);
    }

    @Test void staleAndInvalidEpochRejectedAndCurrentAccepted() throws Exception {
        assertEquals(409,call("POST","0",2).getStatus());
        assertEquals(409,call("POST","invalid",2).getStatus());
        assertEquals(409,call("POST","-2",2).getStatus());
        assertEquals(200,call("PATCH","2",2).getStatus());
        verify(chain,times(1)).doFilter(any(),any());
    }

    @Test void readOnlyRequestsDoNotRequireEpoch() throws Exception {
        assertEquals(200,call("GET",null,4).getStatus());
        verify(chain).doFilter(any(),any());
        verifyNoInteractions(db);
    }
}
