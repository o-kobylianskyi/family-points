package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.delegation.DelegatingOAuth2TokenValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Only authenticates identities explicitly linked by verified Google subject. */
@Service
public class GoogleIdentityService {
    private final JdbcTemplate jdbc;
    private final UserAccountRepository accounts;
    private final String clientId;

    public GoogleIdentityService(JdbcTemplate jdbc, UserAccountRepository accounts,
            @Value("${familypoints.google.client-id:}") String clientId) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.clientId = clientId;
    }

    public String resolveUsername(String idToken) {
        if (clientId.isBlank() || idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google login unavailable");
        }
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
            var issuer = new JwtClaimValidator<String>("iss",
                    value -> "https://accounts.google.com".equals(value) || "accounts.google.com".equals(value));
            var audience = new JwtClaimValidator<List<String>>("aud",
                    value -> value != null && value.contains(clientId));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(), issuer, audience));
            Jwt verified = decoder.decode(idToken);
            String subject = verified.getSubject();
            if (subject == null || subject.isBlank() || subject.length() > 255) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google subject");
            }
            List<Long> ids = jdbc.queryForList("""
                    SELECT account_id FROM account_external_identities
                    WHERE provider = 'google' AND provider_subject = ?
                    """, Long.class, subject);
            if (ids.size() != 1) throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Google account is not linked");
            var account = accounts.findById(ids.get(0))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            if (!account.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            return account.getUsername();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
        }
    }
}
