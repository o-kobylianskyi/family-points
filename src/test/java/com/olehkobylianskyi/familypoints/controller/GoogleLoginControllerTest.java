package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.JwtService;
import com.olehkobylianskyi.familypoints.service.GoogleIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleLoginControllerTest {
    final GoogleIdentityService google = mock(GoogleIdentityService.class);
    final UserDetailsService users = mock(UserDetailsService.class);
    final JwtService jwt = mock(JwtService.class);

    @Test
    void disabledByDefaultReturns503WithoutInspectingToken() {
        var controller = new GoogleLoginController(google, users, jwt, false);
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> controller.login(new GoogleLoginController.Request("id-token")))
                .getStatusCode().value());
        verifyNoInteractions(google, users, jwt);
    }

    @Test
    void enabledIssuesFamilylessSessionOnlyAfterVerifiedSubjectLookup() {
        when(google.resolveUsername("verified-google-id-token")).thenReturn("global-account");
        var principal = User.withUsername("global-account").password("hash").authorities("USER").build();
        when(users.loadUserByUsername("global-account")).thenReturn(principal);
        when(jwt.generateAccessToken(principal)).thenReturn("signed-familyless-jwt");
        when(jwt.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        var response = new GoogleLoginController(google, users, jwt, true)
                .login(new GoogleLoginController.Request("verified-google-id-token"));
        assertEquals("signed-familyless-jwt", response.getAccessToken());
        verify(jwt).generateAccessToken(principal);
        verify(google).resolveUsername("verified-google-id-token");
        verifyNoMoreInteractions(google);
    }
}
