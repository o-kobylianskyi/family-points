package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.RewardAvailabilityMode;
import java.time.Instant;
import java.util.List;

/** scheduleOpenNow is TIME ONLY; normal balance/prerequisites/approval remain separate. */
public record RewardAvailabilityResponse(
        Long rewardId,
        RewardAvailabilityMode mode,
        String workspaceTimeZone,
        Instant startsAt,
        Instant endsAt,
        List<RewardWeeklyWindowRequest> weeklyWindows,
        boolean scheduleOpenNow,
        Instant availableUntil,
        Instant nextOpenAt,
        String reason
) {}
