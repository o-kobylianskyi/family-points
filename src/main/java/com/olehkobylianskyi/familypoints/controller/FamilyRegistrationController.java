package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.FamilyRegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class FamilyRegistrationController {
    private final FamilyRegistrationService registration;
    private final boolean enabled;

    public FamilyRegistrationController(FamilyRegistrationService registration,
            @Value("${familypoints.registration.enabled:false}") boolean enabled) {
        this.registration = registration;
        this.enabled = enabled;
    }

    public record Request(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,50}") String username,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotBlank @Size(max = 100) String familyName,
            @NotBlank @Size(max = 100) String ownerName) {}

    @PostMapping("/register-family")
    public FamilyRegistrationService.CreatedFamily register(@Valid @RequestBody Request request) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Family registration is currently disabled");
        return registration.register(request.username(), request.password(),
                request.familyName(), request.ownerName());
    }
}
