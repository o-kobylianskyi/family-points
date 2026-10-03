package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.TaskInstance;
import com.olehkobylianskyi.familypoints.entity.TaskInstanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskInstanceRepository
        extends JpaRepository<TaskInstance, Long> {

    List<TaskInstance>
    findByMemberIdAndScheduledDateOrderByIdAsc(
            Long memberId,
            LocalDate scheduledDate
    );

    Optional<TaskInstance>
    findByTaskDefinitionIdAndScheduledDate(
            Long taskDefinitionId,
            LocalDate scheduledDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ti
            from TaskInstance ti
            where ti.id = :instanceId
              and ti.taskDefinition.workspace.id = :workspaceId
            """)
    Optional<TaskInstance> findByIdAndWorkspaceIdForUpdate(
            @Param("instanceId") Long instanceId,
            @Param("workspaceId") Long workspaceId
    );

    boolean existsByTaskDefinitionIdAndMemberIdAndScheduledDate(
            Long taskDefinitionId, Long memberId, LocalDate scheduledDate
    );

    boolean existsByTaskDefinitionIdAndScheduledDate(Long taskDefinitionId, LocalDate scheduledDate);

    List<TaskInstance> findByTaskDefinitionIdAndStatus(Long taskDefinitionId, TaskInstanceStatus status);

    @Query("""
        select ti
        from TaskInstance ti
        join fetch ti.taskDefinition td
        join fetch ti.member m
        where ti.scheduledDate <= :date
          and ti.status in :statuses
          and td.dueTime is not null
        order by ti.scheduledDate asc, ti.id asc
        """)
    List<TaskInstance> findCandidatesForDeadlineProcessing(
            @Param("date") LocalDate date,
            @Param("statuses") List<TaskInstanceStatus> statuses
    );
}