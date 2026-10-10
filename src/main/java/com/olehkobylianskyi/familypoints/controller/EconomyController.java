package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.EconomySettings;
import com.olehkobylianskyi.familypoints.entity.PointType;
import com.olehkobylianskyi.familypoints.service.EconomyService;
import com.olehkobylianskyi.familypoints.repository.PointTypeNameFormRepository;
import com.olehkobylianskyi.familypoints.repository.PointTypeRepository;
import com.olehkobylianskyi.familypoints.entity.PointTypeNameForm;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Locale;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/economy")
public class EconomyController {

    private final EconomyService economyService;
    private final PointTypeNameFormRepository nameForms;
    private final PointTypeRepository pointTypes;

    public EconomyController(
            EconomyService economyService,
            PointTypeNameFormRepository nameForms,
            PointTypeRepository pointTypes
    ) {
        this.economyService = economyService;
        this.nameForms = nameForms;
        this.pointTypes = pointTypes;
    }

    @GetMapping
    public EconomySettingsResponse getSettings(
            @PathVariable Long workspaceId
    ) {
        return EconomySettingsResponse.from(
                economyService.getOrCreateSettings(workspaceId)
        );
    }

    @GetMapping("/point-types")
    public List<PointTypeResponse> getPointTypes(
            @PathVariable Long workspaceId
    ) {
        return economyService
                .getActivePointTypes(workspaceId)
                .stream()
                .map(PointTypeResponse::from)
                .toList();
    }

    @PostMapping("/point-types")
    public ResponseEntity<PointTypeResponse> createPointType(
            @PathVariable Long workspaceId,
            @Valid @RequestBody PointTypeCreateRequest request
    ) {
        PointType pointType =
                economyService.createPointType(
                        workspaceId,
                        request.getCode(),
                        request.getName(),
                        request.getSortOrder()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PointTypeResponse.from(pointType));
    }

    @PostMapping("/mode/multi")
    public EconomySettingsResponse enableMultiMode(
            @PathVariable Long workspaceId,
            @RequestParam Long defaultPointTypeId
    ) {
        EconomySettings settings =
                economyService.enableMultiTypeMode(
                        workspaceId,
                        defaultPointTypeId
                );

        return EconomySettingsResponse.from(settings);
    }

    @PostMapping("/mode/simple")
    public EconomySettingsResponse enableSimpleMode(
            @PathVariable Long workspaceId
    ) {
        return EconomySettingsResponse.from(
                economyService.enableSimpleMode(workspaceId)
        );
    }

    @GetMapping("/point-types/{pointTypeId}/name-forms")
    @PreAuthorize("@workspaceSecurity.canAccessWorkspace(#workspaceId)")
    public List<PointTypeNameFormResponse> getNameForms(
            @PathVariable Long workspaceId, @PathVariable Long pointTypeId) {
        pointTypes.findByIdAndWorkspaceId(pointTypeId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Point type not found"));
        return nameForms.findByPointTypeId(pointTypeId).stream()
                .map(PointTypeNameFormResponse::from).toList();
    }

    @PutMapping("/point-types/{pointTypeId}/name-forms/{language}")
    @PreAuthorize("@workspaceSecurity.canManageEconomy(#workspaceId)")
    public PointTypeNameFormResponse saveNameForms(
            @PathVariable Long workspaceId, @PathVariable Long pointTypeId,
            @PathVariable String language, @Valid @RequestBody PointTypeNameFormRequest request) {
        String lang = language.toLowerCase(Locale.ROOT);
        if (!List.of("uk", "ru", "en", "de").contains(lang)) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        PointType type = pointTypes.findByIdAndWorkspaceId(pointTypeId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Point type not found"));
        PointTypeNameForm form = nameForms.findByPointTypeIdAndLanguage(pointTypeId, lang)
                .orElseGet(() -> new PointTypeNameForm(type, lang,
                        request.one().trim(), request.few().trim(), request.many().trim()));
        form.setForms(request.one().trim(), request.few().trim(), request.many().trim());
        return PointTypeNameFormResponse.from(nameForms.save(form));
    }
}