package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GlobalAccountRegistrationServiceTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final EmailTokenService email = mock(EmailTokenService.class);
    private final GlobalAccountRegistrationService service =
            new GlobalAccountRegistrationService(accounts, encoder, email);

    @Test
    void rejectsMissingOrInvalidEmailBeforePersisting() {
        assertThrows(ResponseStatusException.class,
                () -> service.register("demo", "", "long-enough-password"));
        assertThrows(ResponseStatusException.class,
                () -> service.register("demo", "invalid-email", "long-enough-password"));
        assertThrows(ResponseStatusException.class,
                () -> service.register("bad name", "demo@example.com", "long-enough-password"));
        assertThrows(ResponseStatusException.class,
                () -> service.register("demo", "demo@example.com", "short"));
        verifyNoInteractions(accounts, encoder, email);
    }

    @Test
    void rejectsOnlyExistingUsername() {
        when(accounts.existsByUsername("demo")).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.register("demo", "demo@example.com", "long-enough-password")).getStatusCode().value());
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void acceptsEightCharacterPassword() {
        when(encoder.encode("abcdefgh")).thenReturn("encoded");
        when(accounts.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            try {
                var id = UserAccount.class.getDeclaredField("id");
                id.setAccessible(true);
                id.set(account, 10L);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            return account;
        });
        service.register("newname", "parent@example.com", "abcdefgh");
        verify(email).sendVerification(10L, "parent@example.com");
    }

    @Test
    void rejectsSevenCharacterPassword() {
        assertThrows(ResponseStatusException.class,
                () -> service.register("demo", "demo@example.com", "abcdefg"));
        verifyNoInteractions(accounts, encoder, email);
    }

    @Test
    void newAccountDisabledUntilEmailVerification() {
        when(encoder.encode(anyString())).thenReturn("encoded-password");
        when(accounts.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            assertNull(account.getWorkspaceMember());
            assertEquals("encoded-password", account.getPasswordHash());
            assertEquals("demo@example.com", account.getEmail());
            assertFalse(account.isEnabled());
            assertNull(account.getEmailVerifiedAt());
            try {
                var id = UserAccount.class.getDeclaredField("id");
                id.setAccessible(true);
                id.set(account, 123L);
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            return account;
        });
        var created = service.register("demo", "DEMO@EXAMPLE.COM", "long-enough-password");
        assertEquals(123L, created.accountId());
        verify(email).sendVerification(123L, "demo@example.com");
    }
}
