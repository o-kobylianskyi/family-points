package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.LoginResponse;
import com.olehkobylianskyi.familypoints.security.JwtService;
import com.olehkobylianskyi.familypoints.service.GoogleIdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/google")
public class GoogleLoginController {
    private final GoogleIdentityService google;
    private final UserDetailsService users;
    private final JwtService tokens;
    private final boolean enabled;

    public GoogleLoginController(GoogleIdentityService google, UserDetailsService users,
            JwtService tokens, @Value("${familypoints.google.enabled:false}") boolean enabled) {
        this.google = google;
        this.users = users;
        this.tokens = tokens;
        this.enabled = enabled;
    }

    public record Request(@NotBlank String idToken) {}

    @PostMapping
    public LoginResponse login(@Valid @RequestBody Request body) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google login disabled");
        var principal = users.loadUserByUsername(google.resolveUsername(body.idToken()));
        if (!principal.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return new LoginResponse(tokens.generateAccessToken(principal), "Bearer",
                tokens.getAccessTokenExpirationSeconds());
    }
}
