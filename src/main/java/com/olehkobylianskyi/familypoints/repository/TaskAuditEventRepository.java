package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.TaskAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAuditEventRepository extends JpaRepository<TaskAuditEvent, Long> {

    List<TaskAuditEvent> findByTaskDefinitionIdOrderByOccurredAtAscIdAsc(Long taskDefinitionId);

    List<TaskAuditEvent> findByTaskInstanceIdOrderByOccurredAtAscIdAsc(Long taskInstanceId);
}
