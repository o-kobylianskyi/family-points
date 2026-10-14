package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

/** New password accounts cannot authenticate before the email verification. */
@Service
public class GlobalAccountRegistrationService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final EmailTokenService verification;

    public GlobalAccountRegistrationService(UserAccountRepository accounts, PasswordEncoder encoder,
                                            EmailTokenService verification) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.verification = verification;
    }

    public record Created(Long accountId) {}

    @Transactional
    public Created register(String username, String email, String password) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")
                || email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || email.length() > 254
                || password == null || password.length() < 8 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration values");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByUsername(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username unavailable");

        var account = new UserAccount(username, encoder.encode(password), null);
        account.setEmail(normalized);
        account.setEnabled(false);
        account = accounts.saveAndFlush(account);
        verification.sendVerification(account.getId(), normalized);
        return new Created(account.getId());
    }
}
