package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoogleAccountLinkServiceTest {
    final GoogleTokenVerifier verifier = mock(GoogleTokenVerifier.class);
    final CurrentUserService current = mock(CurrentUserService.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final GoogleAccountLinkService service =
            new GoogleAccountLinkService(verifier, current, passwords, jdbc);

    @Test
    void requiresPasswordBeforeVerifyingGoogleToken() {
        var account = new UserAccount("global", "stored-hash", null);
        when(current.getCurrentAccount()).thenReturn(account);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.link("id-token", "wrong")).getStatusCode().value());
        verifyNoInteractions(verifier, jdbc);
    }

    @Test
    void linksOnlyCryptographicallyVerifiedSubject() {
        var account = mock(UserAccount.class);
        when(account.isEnabled()).thenReturn(true);
        when(account.getId()).thenReturn(17L);
        when(account.getPasswordHash()).thenReturn("stored-hash");
        when(current.getCurrentAccount()).thenReturn(account);
        when(passwords.matches("correct", "stored-hash")).thenReturn(true);
        when(verifier.verifiedSubject("id-token")).thenReturn("google-subject");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(17L),
                eq("google-subject"))).thenReturn(0);
        service.link("id-token", "correct");
        verify(jdbc).update(anyString(), eq(17L), eq("google-subject"));
    }
}
