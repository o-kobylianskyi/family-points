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

    public EconomyService(
            EconomySettingsRepository settingsRepository,
            WorkspaceRepository workspaceRepository,
            PointTypeRepository pointTypeRepository
    ) {
        this.settingsRepository = settingsRepository;
        this.workspaceRepository = workspaceRepository;
        this.pointTypeRepository = pointTypeRepository;
    }

    @Transactional
    public EconomySettings getOrCreateSettings(Long workspaceId) {

        return settingsRepository
                .findByWorkspaceId(workspaceId)
                .orElseGet(() -> createDefaultSettings(workspaceId));
    }

    @Transactional(readOnly = true)
    public List<PointType> getActivePointTypes(Long workspaceId) {

        getWorkspaceOrThrow(workspaceId);

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

        String normalizedCode =
                code.trim().toUpperCase();

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