package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.FirstFamilyOnboardingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/onboarding/first-family")
public class FirstFamilyOnboardingController {
    private final FirstFamilyOnboardingService onboarding;
    private final boolean enabled;

    public FirstFamilyOnboardingController(FirstFamilyOnboardingService onboarding,
            @Value("${familypoints.first-family.enabled:false}") boolean enabled) {
        this.onboarding = onboarding;
        this.enabled = enabled;
    }

    public record Request(@NotBlank @Size(max=100) String familyName,
                          @NotBlank @Size(max=100) String ownerName) {}

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public FirstFamilyOnboardingService.Created create(@Valid @RequestBody Request request) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        return onboarding.create(request.familyName(), request.ownerName());
    }
}
