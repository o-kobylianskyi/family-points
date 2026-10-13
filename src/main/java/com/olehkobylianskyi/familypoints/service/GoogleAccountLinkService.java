package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Linking requires both a logged-in account and proof of its password. */
@Service
public class GoogleAccountLinkService {
    private final GoogleTokenVerifier google;
    private final CurrentUserService current;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;

    public GoogleAccountLinkService(GoogleTokenVerifier google, CurrentUserService current,
                                    PasswordEncoder passwords, JdbcTemplate jdbc) {
        this.google = google;
        this.current = current;
        this.passwords = passwords;
        this.jdbc = jdbc;
    }

    @Transactional
    public void link(String idToken, String currentPassword) {
        var account = current.getCurrentAccount();
        if (!account.isEnabled() || currentPassword == null
                || !passwords.matches(currentPassword, account.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reauthentication required");
        String subject = google.verifiedSubject(idToken);
        Integer attached = jdbc.queryForObject("""
                SELECT count(*) FROM account_external_identities
                WHERE account_id = ? AND provider = 'google' AND provider_subject = ?
                """, Integer.class, account.getId(), subject);
        if (attached != null && attached > 0) return;
        try {
            jdbc.update("""
                    INSERT INTO account_external_identities(account_id, provider, provider_subject)
                    VALUES (?, 'google', ?)
                    """, account.getId(), subject);
        } catch (DuplicateKeyException conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Google identity is already linked or this account has another Google identity");
        }
    }
}
