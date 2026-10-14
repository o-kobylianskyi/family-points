package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;

/** Creates a separate global account for a verified Google identity; never links by email. */
@Service
public class GoogleRegistrationService {
    private final GoogleTokenVerifier google;
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final JdbcTemplate db;
    private final SecureRandom random = new SecureRandom();

    public GoogleRegistrationService(GoogleTokenVerifier google, UserAccountRepository accounts,
                                     PasswordEncoder encoder, JdbcTemplate db) {
        this.google = google;
        this.accounts = accounts;
        this.encoder = encoder;
        this.db = db;
    }

    @Transactional
    public String register(String idToken) {
        var verified = google.verifiedToken(idToken);
        String subject = verified.getSubject();
        String email = verified.getClaimAsString("email");
        if (!Boolean.TRUE.equals(verified.getClaim("email_verified"))
                || email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || email.length() > 254)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verified Google email required");
        email = email.toLowerCase(Locale.ROOT);
        String username = "g_" + subjectDigest(subject).substring(0, 32);
        if (accounts.existsByUsername(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Google identity already registered");
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        var account = new UserAccount(username, encoder.encode(HexFormat.of().formatHex(secret)), null);
        account.setEmail(email);
        account.setEmailVerifiedAt(OffsetDateTime.now());
        account = accounts.saveAndFlush(account);
        try {
            db.update("""
                    INSERT INTO account_external_identities(account_id,provider,provider_subject)
                    VALUES (?, 'google', ?)
                    """, account.getId(), subject);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Google identity unavailable");
        }
        return username;
    }

    private static String subjectDigest(String subject) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(subject.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
