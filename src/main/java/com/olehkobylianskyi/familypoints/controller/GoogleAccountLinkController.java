package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.GoogleAccountLinkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account/google")
public class GoogleAccountLinkController {
    private final GoogleAccountLinkService links;
    private final boolean enabled;

    public GoogleAccountLinkController(GoogleAccountLinkService links,
            @Value("${familypoints.google.linking-enabled:false}") boolean enabled) {
        this.links = links;
        this.enabled = enabled;
    }

    public record LinkRequest(@NotBlank String idToken, @NotBlank String currentPassword) {}


    public record SetPasswordRequest(@NotBlank String idToken, @NotBlank String newPassword, @NotBlank String username) {}

    public record UsernameSuggestion(String username) {}

    @GetMapping("/suggested-username")
    @PreAuthorize("isAuthenticated()")
    public UsernameSuggestion suggestedUsername() {
        return new UsernameSuggestion(links.suggestedUsername());
    }

    @PostMapping("/set-password")
    @PreAuthorize("isAuthenticated()")
    public void setPassword(@Valid @RequestBody SetPasswordRequest request) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        links.setPasswordWithGoogleVerification(request.idToken(), request.newPassword(), request.username());
    }

    @PostMapping("/link")
    @PreAuthorize("isAuthenticated()")
    public void link(@Valid @RequestBody LinkRequest request) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        links.link(request.idToken(), request.currentPassword());
    }
}
