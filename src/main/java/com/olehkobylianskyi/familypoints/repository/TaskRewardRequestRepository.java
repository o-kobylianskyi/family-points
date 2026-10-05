package com.olehkobylianskyi.familypoints.repository;

import com.olehkobylianskyi.familypoints.entity.TaskRewardRequest;
import com.olehkobylianskyi.familypoints.entity.TaskRewardRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRewardRequestRepository extends JpaRepository<TaskRewardRequest, Long> {
    List<TaskRewardRequest> findByTaskInstanceIdOrderByCreatedAtDesc(Long taskInstanceId);
    List<TaskRewardRequest> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<TaskRewardRequest> findFirstByTaskInstanceIdAndStatusOrderByCreatedAtDesc(Long taskInstanceId, TaskRewardRequestStatus status);
    boolean existsByTaskInstanceIdAndRequestedByIdAndStatusIn(
            Long taskInstanceId,
            Long requestedById,
            List<TaskRewardRequestStatus> statuses
    );
    Optional<TaskRewardRequest> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
