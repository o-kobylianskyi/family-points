package com.olehkobylianskyi.familypoints.dto;

import com.olehkobylianskyi.familypoints.entity.TaskDefinition;
import com.olehkobylianskyi.familypoints.entity.TaskInstance;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Read-only calendar projection. A null instanceId means that no database
 * record exists yet; it must not be treated as a completed/claimable instance.
 */
public record TaskScheduleOccurrenceResponse(
        String occurrenceKey,
        Long taskDefinitionId,
        Long instanceId,
        Long memberId,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        String title,
        String status,
        Integer rewardAmount,
        Integer rewardReputationAmount
) {
    public static TaskScheduleOccurrenceResponse planned(TaskDefinition definition, Long memberId, LocalDate date) {
        return plannedSlot(definition, memberId, date, "default", definition.getDueTime());
    }

    public static TaskScheduleOccurrenceResponse plannedSlot(TaskDefinition definition, Long memberId, LocalDate date, String slotKey, LocalTime time) {
        return new TaskScheduleOccurrenceResponse(
                definition.getId() + ":" + date + ":" + slotKey,
                definition.getId(), null, memberId, date, time,
                definition.getTitle(), "PLANNED", definition.getRewardAmount(),
                definition.getRewardReputationAmount());
    }

    public static TaskScheduleOccurrenceResponse existing(TaskInstance instance) {
        return new TaskScheduleOccurrenceResponse(
                instance.getTaskDefinition().getId() + ":" + instance.getScheduledDate() + ":" + instance.getSlotKey(),
                instance.getTaskDefinition().getId(), instance.getId(), instance.getMember().getId(),
                instance.getScheduledDate(), instance.getDueTime(),
                instance.getTitle(), instance.getStatus().name(),
                instance.getRewardAmount(), instance.getRewardReputationAmount());
    }
}
