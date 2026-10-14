package com.olehkobylianskyi.familypoints.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;

/** Stores only SHA-256 hashes, never plaintext mail tokens. */
@Service
public class EmailTokenService {
    private final JdbcTemplate db;
    private final JavaMailSender mail;
    private final String from;
    private final String publicWebUrl;
    private final SecureRandom random = new SecureRandom();

    public EmailTokenService(JdbcTemplate db, JavaMailSender mail,
            @Value("${familypoints.mail.from:}") String from,
            @Value("${familypoints.web-url:http://localhost:5173}") String publicWebUrl) {
        this.db = db;
        this.mail = mail;
        this.from = from;
        this.publicWebUrl = publicWebUrl;
    }

    public String generateToken(long accountId, String purpose, int validityMinutes) {
        Integer recent = db.queryForObject("""
            SELECT count(*) FROM account_email_tokens
            WHERE account_id = ? AND purpose = ?
              AND created_at > CURRENT_TIMESTAMP - INTERVAL '2 minutes'
            """, Integer.class, accountId, purpose);
        if (recent != null && recent > 0)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
        byte[] entropy = new byte[32];
        random.nextBytes(entropy);
        String token = HexFormat.of().formatHex(entropy);
        db.update("""
            INSERT INTO account_email_tokens(account_id, token_hash, purpose, expires_at)
            VALUES (?, ?, ?, ?)
            """, accountId, hash(token), purpose, OffsetDateTime.now().plusMinutes(validityMinutes));
        return token;
    }

    public void sendVerification(long accountId, String email) {
        if (from.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP is not configured");
        String token = generateToken(accountId, "VERIFY_EMAIL", 24 * 60);
        send(email, "Confirm your FamilyPoints email",
                "Open this link to verify your address:\n" +
                publicWebUrl + "/verify-email?token=" + token + "\n\nExpires in 24 hours.");
    }

    public void sendReset(long accountId, String email) {
        if (from.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP is not configured");
        String token = generateToken(accountId, "RESET_PASSWORD", 30);
        send(email, "Reset your FamilyPoints password",
                "Open this link to reset your password:\n" +
                publicWebUrl + "/reset-password?token=" + token + "\n\nExpires in 30 minutes.");
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mail.send(message);
    }

    @Transactional
    public Long consume(String plaintextToken, String purpose) {
        if (plaintextToken == null || !plaintextToken.matches("[0-9a-f]{64}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        var ids = db.queryForList("""
            SELECT id, account_id FROM account_email_tokens
            WHERE token_hash = ? AND purpose = ? AND consumed_at IS NULL AND expires_at > CURRENT_TIMESTAMP
            FOR UPDATE
            """, hash(plaintextToken), purpose);
        if (ids.size() != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired or already used");
        Long id = ((Number) ids.get(0).get("id")).longValue();
        Long accountId = ((Number) ids.get(0).get("account_id")).longValue();
        db.update("UPDATE account_email_tokens SET consumed_at = CURRENT_TIMESTAMP WHERE id = ?", id);
        return accountId;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
