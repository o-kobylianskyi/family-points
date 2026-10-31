package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PointTypeNameFormRequest(
    @NotBlank @Size(max = 100) String one,
    @NotBlank @Size(max = 100) String few,
    @NotBlank @Size(max = 100) String many
) {}
