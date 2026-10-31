package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Resolves only prelinked Google subjects after cryptographic token verification. */
@Service
public class GoogleIdentityService {
    private final JdbcTemplate jdbc;
    private final UserAccountRepository accounts;
    private final GoogleTokenVerifier verifier;

    public GoogleIdentityService(JdbcTemplate jdbc, UserAccountRepository accounts,
            GoogleTokenVerifier verifier) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.verifier = verifier;
    }

    public String resolveUsername(String idToken) {
        String subject = verifier.verifiedSubject(idToken);
        List<Long> ids = jdbc.queryForList("""
                SELECT account_id FROM account_external_identities
                WHERE provider = 'google' AND provider_subject = ?
                """, Long.class, subject);
        if (ids.size() != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Google identity not linked");
        var account = accounts.findById(ids.get(0))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!account.isEnabled())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return account.getUsername();
    }
}
