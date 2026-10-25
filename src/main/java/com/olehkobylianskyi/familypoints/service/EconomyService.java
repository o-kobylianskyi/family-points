package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.EconomySettingsRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceRepository;
import com.olehkobylianskyi.familypoints.repository.PointTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EconomyService {

    private final EconomySettingsRepository settingsRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PointTypeRepository pointTypeRepository;
    private final StandardCurrencyForms standardCurrencyForms;

    public EconomyService(
            EconomySettingsRepository settingsRepository,
            WorkspaceRepository workspaceRepository,
            PointTypeRepository pointTypeRepository,
            StandardCurrencyForms standardCurrencyForms
    ) {
        this.settingsRepository = settingsRepository;
        this.workspaceRepository = workspaceRepository;
        this.pointTypeRepository = pointTypeRepository;
        this.standardCurrencyForms = standardCurrencyForms;
    }

    @Transactional
    public EconomySettings getOrCreateSettings(Long workspaceId) {

        EconomySettings settings = settingsRepository.findByWorkspaceId(workspaceId)
                .orElseGet(() -> createDefaultSettings(workspaceId));
        ensureStandardCurrencies(getWorkspaceOrThrow(workspaceId),
                "POINTS".equals(settings.getDefaultPointType().getCode())
                        ? settings.getDefaultPointType() : null);
        return settings;
    }

    @Transactional
    public List<PointType> getActivePointTypes(Long workspaceId) {
        getOrCreateSettings(workspaceId);
        return pointTypeRepository
                .findByWorkspaceIdAndActiveTrueOrderBySortOrderAsc(
                        workspaceId
                );
    }

    @Transactional
    public PointType createPointType(
            Long workspaceId,
            String code,
            String name,
            int sortOrder
    ) {
        Workspace workspace = getWorkspaceOrThrow(workspaceId);

        if (code == null || !code.trim().toUpperCase(java.util.Locale.ROOT).matches("[A-Z][A-Z0-9_]{1,49}")) {
            throw new IllegalArgumentException("Invalid currency code");
        }
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new IllegalArgumentException("Invalid currency name");
        }
        String normalizedCode = code.trim().toUpperCase(java.util.Locale.ROOT);

        if (pointTypeRepository
                .findByWorkspaceIdAndCode(
                        workspaceId,
                        normalizedCode
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Point type with code "
                            + normalizedCode
                            + " already exists"
            );
        }

        PointType pointType =
                new PointType(
                        workspace,
                        normalizedCode,
                        name.trim(),
                        sortOrder
                );

        return pointTypeRepository.save(pointType);
    }

    @Transactional
    public List<PointType> getAllPointTypes(Long workspaceId) {
        getOrCreateSettings(workspaceId);
        return pointTypeRepository.findByWorkspaceIdOrderBySortOrderAsc(workspaceId);
    }

    @Transactional
    public PointType updatePointType(Long workspaceId, Long pointTypeId, String name,
                                     int sortOrder, boolean active) {
        PointType type = getPointTypeOrThrow(workspaceId, pointTypeId);
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new IllegalArgumentException("Currency name must contain 1–100 characters");
        }
        if (!active) {
            EconomySettings settings = getOrCreateSettings(workspaceId);
            if (settings.getDefaultPointType().getId().equals(type.getId())) {
                throw new IllegalArgumentException("Cannot deactivate the default currency");
            }
        }
        type.setName(name.trim());
        type.setSortOrder(sortOrder);
        type.setActive(active);
        return pointTypeRepository.save(type);
    }

    @Transactional
    public EconomySettings enableMultiTypeMode(
            Long workspaceId,
            Long defaultPointTypeId
    ) {
        EconomySettings settings =
                getOrCreateSettings(workspaceId);

        PointType defaultPointType =
                getPointTypeOrThrow(
                        workspaceId,
                        defaultPointTypeId
                );

        if (!defaultPointType.isActive()) {
            throw new IllegalArgumentException(
                    "Default point type must be active"
            );
        }

        settings.setMode(EconomyMode.MULTI_TYPE);
        settings.setDefaultPointType(defaultPointType);

        return settings;
    }

    @Transactional
    public EconomySettings enableSimpleMode(
            Long workspaceId
    ) {
        EconomySettings settings =
                getOrCreateSettings(workspaceId);

        PointType points =
                pointTypeRepository
                        .findByWorkspaceIdAndCode(
                                workspaceId,
                                "POINTS"
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Default POINTS type not found"
                                )
                        );

        points.setActive(true);

        settings.setMode(EconomyMode.SIMPLE);
        settings.setDefaultPointType(points);

        return settings;
    }

    private EconomySettings createDefaultSettings(
            Long workspaceId
    ) {
        Workspace workspace =
                getWorkspaceOrThrow(workspaceId);

        PointType points =
                pointTypeRepository
                        .findByWorkspaceIdAndCode(
                                workspaceId,
                                "POINTS"
                        )
                        .orElseGet(() ->
                                pointTypeRepository.save(
                                        new PointType(
                                                workspace,
                                                "POINTS",
                                                "Points",
                                                0
                                        )
                                )
                        );

        EconomySettings settings =
                new EconomySettings(
                        workspace,
                        EconomyMode.SIMPLE,
                        points
                );

        return settingsRepository.save(settings);
    }

    private void ensureStandardCurrencies(Workspace workspace, PointType defaultPoints) {
        PointType points = defaultPoints != null ? defaultPoints : ensureStandardPointType(workspace, "POINTS", "Points", 0);
        PointType copper = ensureStandardPointType(workspace, "COPPER", "Copper", 10);
        PointType silver = ensureStandardPointType(workspace, "SILVER", "Silver", 20);
        PointType gold = ensureStandardPointType(workspace, "GOLD", "Gold", 30);
        standardCurrencyForms.ensureFor(points);
        standardCurrencyForms.ensureFor(copper);
        standardCurrencyForms.ensureFor(silver);
        standardCurrencyForms.ensureFor(gold);
    }

    private PointType ensureStandardPointType(Workspace workspace, String code, String name, int order) {
        return pointTypeRepository.findByWorkspaceIdAndCode(workspace.getId(), code)
                .orElseGet(() -> pointTypeRepository.save(new PointType(workspace, code, name, order)));
    }

    private Workspace getWorkspaceOrThrow(Long workspaceId) {
        return workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace "
                                        + workspaceId
                                        + " not found"
                        )
                );
    }

    private PointType getPointTypeOrThrow(
            Long workspaceId,
            Long pointTypeId
    ) {
        return pointTypeRepository
                .findByIdAndWorkspaceId(
                        pointTypeId,
                        workspaceId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Point type "
                                        + pointTypeId
                                        + " not found in workspace "
                                        + workspaceId
                        )
                );
    }
}