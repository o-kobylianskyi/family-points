package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleRegistrationServiceTest {
    final GoogleTokenVerifier verifier = mock(GoogleTokenVerifier.class);
    final UserAccountRepository accounts = mock(UserAccountRepository.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final JdbcTemplate db = mock(JdbcTemplate.class);
    final GoogleRegistrationService registration =
            new GoogleRegistrationService(verifier, accounts, passwords, db);

    @Test
    void rejectsUnverifiedGoogleEmail() {
        Jwt token = mock(Jwt.class);
        when(verifier.verifiedToken("id")).thenReturn(token);
        when(token.getSubject()).thenReturn("verified-google-sub");
        when(token.getClaimAsString("email")).thenReturn("test@example.com");
        when(token.getClaim("email_verified")).thenReturn(false);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> registration.register("id")).getStatusCode().value());
        verifyNoInteractions(accounts, passwords, db);
    }

    @Test
    void neverSilentlyLinksExistingEmailAccount() {
        Jwt token = mock(Jwt.class);
        when(verifier.verifiedToken("id")).thenReturn(token);
        when(token.getSubject()).thenReturn("google-sub");
        when(token.getClaimAsString("email")).thenReturn("test@example.com");
        when(token.getClaim("email_verified")).thenReturn(true);
        when(accounts.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> registration.register("id")).getStatusCode().value());
        verify(accounts, never()).saveAndFlush(any());
        verifyNoInteractions(db);
    }
}
