package com.olehkobylianskyi.familypoints.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * TASK.scheduledTime/dueAt is a DEADLINE, not a start.
 * REWARD_WINDOW.startsAt/endsAt delimit when acquisition is permitted,
 * not when a purchased entitlement may be used.
 */
public record CalendarEventResponse(
        String kind, String key, LocalDate date, String title, String status,
        Long memberId, Long taskDefinitionId, Long instanceId, Long rewardId,
        LocalTime scheduledTime, Instant dueAt, Instant startsAt, Instant endsAt,
        boolean scheduleOpenNow
) {}
