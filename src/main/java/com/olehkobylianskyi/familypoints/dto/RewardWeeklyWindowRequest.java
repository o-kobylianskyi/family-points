package com.olehkobylianskyi.familypoints.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** ISO dayOfWeek 1=Monday ... 7=Sunday. Intervals are [start,end), no overnight in v1. */
public record RewardWeeklyWindowRequest(String windowId, int dayOfWeek,
        LocalTime localStartTime, LocalTime localEndTime,
        LocalDate effectiveFromDate, LocalDate effectiveToDate, boolean enabled) {}
