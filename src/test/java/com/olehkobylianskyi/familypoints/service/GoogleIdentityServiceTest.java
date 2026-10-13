package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoogleIdentityServiceTest {
    final GoogleTokenVerifier verifier = mock(GoogleTokenVerifier.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final UserAccountRepository accounts = mock(UserAccountRepository.class);
    final GoogleIdentityService service = new GoogleIdentityService(jdbc, accounts, verifier);

    @Test
    void rejectsUnlinkedGoogleSubjectWithoutEmailFallback() {
        when(verifier.verifiedSubject("token")).thenReturn("google-sub");
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("google-sub"))).thenReturn(List.of());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.resolveUsername("token")).getStatusCode().value());
        verifyNoInteractions(accounts);
    }

    @Test
    void resolvesOnlyExistingEnabledAccount() {
        when(verifier.verifiedSubject("token")).thenReturn("google-sub");
        when(jdbc.queryForList(anyString(), eq(Long.class), eq("google-sub"))).thenReturn(List.of(3L));
        var account = new UserAccount("global-user", "hash", null);
        when(accounts.findById(3L)).thenReturn(Optional.of(account));
        assertEquals("global-user", service.resolveUsername("token"));
    }
}
