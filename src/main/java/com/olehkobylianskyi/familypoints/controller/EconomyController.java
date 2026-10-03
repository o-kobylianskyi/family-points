package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.EconomySettings;
import com.olehkobylianskyi.familypoints.entity.PointType;
import com.olehkobylianskyi.familypoints.service.EconomyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/economy")
public class EconomyController {

    private final EconomyService economyService;

    public EconomyController(
            EconomyService economyService
    ) {
        this.economyService = economyService;
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
}