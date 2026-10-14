package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.service.EmailTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;

/** Public token endpoints: no account enumeration in reset requests. */
@RestController
@RequestMapping("/auth/email")
public class EmailAccountController {
    private final EmailTokenService tokens;
    private final UserAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public EmailAccountController(EmailTokenService tokens, UserAccountRepository accounts,
            PasswordEncoder encoder,
            @Value("${familypoints.global-account-registration.enabled:false}") boolean enabled) {
        this.tokens = tokens;
        this.accounts = accounts;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    public record TokenRequest(@NotBlank String token) {}
    public record ResetRequest(@NotBlank String username, @NotBlank @Email String email) {}
    public record NewPasswordRequest(@NotBlank String token, @NotBlank @Size(min=12,max=128) String password) {}

    @PostMapping("/verify")
    @Transactional
    public void verify(@Valid @RequestBody TokenRequest body) {
        requireEnabled();
        var account = accounts.findById(tokens.consume(body.token(), "VERIFY_EMAIL"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (account.getEmail() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        account.setEmailVerifiedAt(OffsetDateTime.now());
        account.setEnabled(true);
        accounts.save(account);
    }

    @PostMapping("/request-verification")
    @Transactional
    public void requestVerification(@Valid @RequestBody ResetRequest body) {
        requireEnabled();
        accounts.findByUsername(body.username()).filter(account -> account.getEmail() != null &&
                account.getEmail().equalsIgnoreCase(body.email().trim())).ifPresent(account -> {
            if (!account.isEnabled() && account.getEmailVerifiedAt() == null)
                tokens.sendVerification(account.getId(), account.getEmail());
        });
    }

    @PostMapping("/request-reset")
    @Transactional
    public void requestReset(@Valid @RequestBody ResetRequest body) {
        requireEnabled();
        accounts.findByUsername(body.username()).filter(account -> account.getEmail() != null &&
                account.getEmail().equalsIgnoreCase(body.email().trim())).ifPresent(account -> {
            if (account.getEmailVerifiedAt() != null && account.isEnabled())
                tokens.sendReset(account.getId(), account.getEmail());
        });
    }

    @PostMapping("/reset-password")
    @Transactional
    public void resetPassword(@Valid @RequestBody NewPasswordRequest body) {
        requireEnabled();
        var account = accounts.findById(tokens.consume(body.token(), "RESET_PASSWORD"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        if (!account.isEnabled() || account.getEmailVerifiedAt() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        account.setPasswordHash(encoder.encode(body.password()));
        accounts.save(account);
    }

    private void requireEnabled() {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
