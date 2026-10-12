package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.TaskScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskScheduleSlotRepository extends JpaRepository<TaskScheduleSlot, Long> {
    List<TaskScheduleSlot> findByTaskDefinitionIdAndEnabledTrueOrderByScheduledTimeAscSlotKeyAsc(Long taskDefinitionId);
}
