package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.EconomyMode;
import com.olehkobylianskyi.familypoints.entity.EconomySettings;

public class EconomySettingsResponse {

    private final EconomyMode mode;
    private final Long defaultPointTypeId;
    private final String defaultPointTypeCode;

    public EconomySettingsResponse(
            EconomyMode mode,
            Long defaultPointTypeId,
            String defaultPointTypeCode
    ) {
        this.mode = mode;
        this.defaultPointTypeId = defaultPointTypeId;
        this.defaultPointTypeCode = defaultPointTypeCode;
    }

    public static EconomySettingsResponse from(
            EconomySettings settings
    ) {
        return new EconomySettingsResponse(
                settings.getMode(),
                settings.getDefaultPointType().getId(),
                settings.getDefaultPointType().getCode()
        );
    }

    public EconomyMode getMode() {
        return mode;
    }

    public Long getDefaultPointTypeId() {
        return defaultPointTypeId;
    }

    public String getDefaultPointTypeCode() {
        return defaultPointTypeCode;
    }
}