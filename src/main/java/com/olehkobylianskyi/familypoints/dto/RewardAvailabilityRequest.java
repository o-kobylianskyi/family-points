package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RewardAvailabilityMode;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record RewardAvailabilityRequest(
        @NotNull RewardAvailabilityMode mode,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        List<RewardWeeklyWindowRequest> weeklyWindows
) {}
