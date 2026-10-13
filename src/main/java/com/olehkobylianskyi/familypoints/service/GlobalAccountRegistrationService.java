package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Creates an identity without implicitly assigning any family permissions. */
@Service
public class GlobalAccountRegistrationService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;

    public GlobalAccountRegistrationService(UserAccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    public record Created(Long accountId) {}

    @Transactional
    public Created register(String username, String password) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")
                || password == null || password.length() < 12 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration values");
        }
        if (accounts.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username unavailable");
        }
        UserAccount account = accounts.save(new UserAccount(username, encoder.encode(password), null));
        return new Created(account.getId());
    }
}
