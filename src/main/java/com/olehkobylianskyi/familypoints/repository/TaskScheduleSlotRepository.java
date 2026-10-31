package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.TaskScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskScheduleSlotRepository extends JpaRepository<TaskScheduleSlot, Long> {
    List<TaskScheduleSlot> findByTaskDefinitionIdOrderByScheduledTimeAscSlotKeyAsc(Long taskDefinitionId);
    Optional<TaskScheduleSlot> findByIdAndTaskDefinitionId(Long id, Long taskDefinitionId);
    Optional<TaskScheduleSlot> findByTaskDefinitionIdAndSlotKey(Long taskDefinitionId, String slotKey);
    Optional<TaskScheduleSlot> findByTaskDefinitionIdAndSlotKeyAndEnabledTrue(Long taskDefinitionId, String slotKey);
    List<TaskScheduleSlot> findByTaskDefinitionIdAndEnabledTrueOrderByScheduledTimeAscSlotKeyAsc(Long taskDefinitionId);
    List<TaskScheduleSlot> findByTaskDefinitionIdIn(List<Long> definitionIds);
}
