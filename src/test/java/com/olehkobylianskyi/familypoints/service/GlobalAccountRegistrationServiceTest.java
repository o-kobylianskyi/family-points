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
    private final GlobalAccountRegistrationService service =
            new GlobalAccountRegistrationService(accounts, encoder);

    @Test
    void rejectsInvalidCredentialsBeforePersisting() {
        assertThrows(ResponseStatusException.class, () -> service.register("bad name", "long-enough-password"));
        assertThrows(ResponseStatusException.class, () -> service.register("valid", "short"));
        verifyNoInteractions(accounts, encoder);
    }

    @Test
    void rejectsExistingUsername() {
        when(accounts.existsByUsername("demo")).thenReturn(true);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.register("demo", "long-enough-password")).getStatusCode().value());
        verify(accounts, never()).save(any());
    }

    @Test
    void createsNoImplicitFamilyLink() {
        when(encoder.encode(anyString())).thenReturn("encoded-password");
        when(accounts.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            assertNull(account.getWorkspaceMember());
            assertEquals("encoded-password", account.getPasswordHash());
            return account;
        });
        var created = service.register("demo", "long-enough-password");
        assertNull(created.accountId()); // Repository mock does not assign a generated database ID.
        verify(accounts).save(any(UserAccount.class));
    }
}
