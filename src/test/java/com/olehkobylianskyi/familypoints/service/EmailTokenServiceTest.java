package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailTokenServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);

    @Test
    void rejectsHttpCallbackOutsideLocalDevBeforeIssuingTokens() {
        var service = new EmailTokenService(db, mail, "no-reply@example.com",
                "http://example.com");
        var error = assertThrows(ResponseStatusException.class,
                () -> service.sendVerification(42L, "parent@example.com"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verifyNoInteractions(db, mail);
    }

    @Test
    void permitsHttpsCallbackAndHashesIssuedToken() {
        when(db.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(0);
        var service = new EmailTokenService(db, mail, "no-reply@example.com",
                "https://family-point.com");
        service.sendVerification(42L, "parent@example.com");
        verify(db).update(contains("INSERT INTO account_email_tokens"),
                eq(42L), argThat(hash -> hash instanceof String value && value.matches("[0-9a-f]{64}")),
                eq("VERIFY_EMAIL"), any());
        verify(mail).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void doesNotIssueTokenDuringCooldown() {
        when(db.queryForObject(anyString(), eq(Integer.class), anyLong(), anyString()))
                .thenReturn(1);
        var service = new EmailTokenService(db, mail, "no-reply@example.com",
                "https://family-point.com");
        var error = assertThrows(ResponseStatusException.class,
                () -> service.sendReset(42L, "parent@example.com"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
        verify(db, never()).update(anyString(), any(Object[].class));
        verifyNoInteractions(mail);
    }

    @Test
    void rejectsInvalidAndExpiredTokens() {
        var service = new EmailTokenService(db, mail, "", "https://family-point.com");
        assertThrows(ResponseStatusException.class, () -> service.consume("invalid", "VERIFY_EMAIL"));
        when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.consume("f".repeat(64), "VERIFY_EMAIL")).getStatusCode());
    }
}
