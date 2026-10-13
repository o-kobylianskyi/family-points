package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.GlobalAccountRegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class GlobalAccountRegistrationController {
    private final GlobalAccountRegistrationService service;
    private final boolean enabled;

    public GlobalAccountRegistrationController(GlobalAccountRegistrationService service,
            @Value("${familypoints.global-account-registration.enabled:false}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    public record RegisterRequest(@NotBlank String username,
                                  @NotBlank @Size(min=12,max=128) String password) {}

    @PostMapping("/register-account")
    public GlobalAccountRegistrationService.Created create(@Valid @RequestBody RegisterRequest body) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        return service.register(body.username(), body.password());
    }
}
