package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PointTypeUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        int sortOrder,
        @NotNull Boolean active
) {}
