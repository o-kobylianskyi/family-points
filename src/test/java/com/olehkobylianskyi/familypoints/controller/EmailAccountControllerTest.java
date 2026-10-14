package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.service.EmailTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailAccountControllerTest {
    final EmailTokenService tokens = mock(EmailTokenService.class);
    final UserAccountRepository accounts = mock(UserAccountRepository.class);
    final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void verifyEnablesOnlyEmailAccountAfterValidToken() {
        var account = new UserAccount("new-account", "hash", null);
        account.setEmail("test@example.com");
        account.setEnabled(false);
        when(tokens.consume("valid", "VERIFY_EMAIL")).thenReturn(7L);
        when(accounts.findById(7L)).thenReturn(Optional.of(account));
        new EmailAccountController(tokens, accounts, encoder, true)
                .verify(new EmailAccountController.TokenRequest("valid"));
        assertTrue(account.isEnabled());
        assertNotNull(account.getEmailVerifiedAt());
        verify(accounts).save(account);
    }

    @Test
    void disabledFeatureCannotConsumeToken() {
        var controller = new EmailAccountController(tokens, accounts, encoder, false);
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> controller.verify(new EmailAccountController.TokenRequest("token")))
                .getStatusCode().value());
        verifyNoInteractions(tokens, accounts, encoder);
    }

    @Test
    void resetRequestForUnknownAccountDoesNotRevealItsExistence() {
        new EmailAccountController(tokens, accounts, encoder, true)
                .requestReset(new EmailAccountController.ResetRequest("notfound@example.com"));
        verify(accounts).findByEmailIgnoreCase("notfound@example.com");
        verifyNoInteractions(tokens);
    }
}
