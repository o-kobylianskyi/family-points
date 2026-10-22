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


    /**
     * Set a password for the authenticated account after re-verifying its own
     * linked Google subject. Never look up account ownership by email.
     */
    @Transactional
    public void setPasswordWithGoogleVerification(String idToken, String newPassword, String username) {
        var account = current.getCurrentAccount();
        if (!account.isEnabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 8-128 characters");

        var verified = google.verifiedToken(idToken);
        if (verified.getIssuedAt() == null
                || verified.getIssuedAt().isBefore(java.time.Instant.now().minusSeconds(300)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Fresh Google verification required");

        Integer match = jdbc.queryForObject("""
                SELECT count(*) FROM account_external_identities
                WHERE account_id = ? AND provider = 'google' AND provider_subject = ?
                """, Integer.class, account.getId(), verified.getSubject());
        if (match == null || match != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Google identity does not belong to current account");

        String requestedUsername = username == null ? "" : username.trim();
        if (!requestedUsername.matches("[A-Za-z0-9._-]{3,64}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_USERNAME");
        if (!requestedUsername.equals(account.getUsername())) {
            Integer existing = jdbc.queryForObject(
                    "SELECT count(*) FROM user_accounts WHERE lower(username) = lower(?) AND id <> ?",
                    Integer.class, requestedUsername, account.getId());
            if (existing != null && existing > 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "USERNAME_TAKEN");
        }
        // Both fields change in one transaction; Google provider-subject link stays intact.
        jdbc.update("UPDATE user_accounts SET username = ?, password_hash = ? WHERE id = ?",
                requestedUsername, passwords.encode(newPassword), account.getId());
    }

    /** Readable suggestion for an already registered Google account. No mutation. */
    public String suggestedUsername() {
        var account = current.getCurrentAccount();
        // An existing custom username always wins over an email-derived suggestion.
        // This also handles a stale /api/me session after password setup changed
        // the username without reissuing the user's JWT.
        if (!account.getUsername().matches("g_[a-f0-9]{32}")) {
            return account.getUsername();
        }
        String email = account.getEmail();
        if (email == null || !email.contains("@")) return account.getUsername();
        String suggestion = email.substring(0, email.indexOf('@'))
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (suggestion.length() > 64) suggestion = suggestion.substring(0, 64);
        if (suggestion.length() < 3) return account.getUsername();
        Integer taken = jdbc.queryForObject(
                "SELECT count(*) FROM user_accounts WHERE lower(username) = lower(?) AND id <> ?",
                Integer.class, suggestion, account.getId());
        return taken != null && taken > 0 ? account.getUsername() : suggestion;
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
