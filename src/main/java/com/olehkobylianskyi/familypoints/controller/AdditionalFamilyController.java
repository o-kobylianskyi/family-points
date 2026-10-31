package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.AdditionalFamilyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** DEV opt-in creation of additional family by existing managing account. */
@RestController
@RequestMapping("/api/account/families")
public class AdditionalFamilyController {
    private final AdditionalFamilyService families;
    private final boolean enabled;

    public AdditionalFamilyController(AdditionalFamilyService families,
            @Value("${familypoints.additional-family.enabled:false}") boolean enabled) {
        this.families = families;
        this.enabled = enabled;
    }

    public record CreateRequest(@NotBlank @Size(max=100) String familyName,
                                @NotBlank @Size(max=100) String ownerName) {}

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_MEMBERS') or hasAuthority('ADMIN_OVERRIDE')")
    public AdditionalFamilyService.Created create(@Valid @RequestBody CreateRequest request) {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Additional family creation disabled");
        return families.create(request.familyName(), request.ownerName());
    }
}
