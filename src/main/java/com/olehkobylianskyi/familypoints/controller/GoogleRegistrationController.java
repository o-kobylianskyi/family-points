package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.LoginResponse;
import com.olehkobylianskyi.familypoints.security.JwtService;
import com.olehkobylianskyi.familypoints.service.GoogleRegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/google/register")
public class GoogleRegistrationController {
    private final GoogleRegistrationService registration;
    private final UserDetailsService users;
    private final JwtService jwt;
    private final boolean enabled;

    public GoogleRegistrationController(GoogleRegistrationService registration, UserDetailsService users,
            JwtService jwt, @Value("${familypoints.google.registration-enabled:false}") boolean enabled) {
        this.registration = registration;
        this.users = users;
        this.jwt = jwt;
        this.enabled = enabled;
    }

    public record Request(@NotBlank String idToken) {}

    @PostMapping
    public LoginResponse register(@Valid @RequestBody Request request) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        var username = registration.register(request.idToken());
        var principal = users.loadUserByUsername(username);
        return new LoginResponse(jwt.generateAccessToken(principal), "Bearer",
                jwt.getAccessTokenExpirationSeconds());
    }
}
