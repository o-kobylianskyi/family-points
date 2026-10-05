package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskRewardNegotiationService {

    private final TaskRewardRequestRepository requests;
    private final TaskInstanceRepository taskInstances;
    private final PointTypeRepository pointTypes;
    private final RewardDefinitionRepository rewards;
    private final RewardRequestRepository rewardRequests;
    private final WorkspaceRepository workspaces;
    private final CurrentUserService currentUser;
    private final PointService pointService;
    private final ReputationService reputationService;

    public TaskRewardNegotiationService(
            TaskRewardRequestRepository requests,
            TaskInstanceRepository taskInstances,
            PointTypeRepository pointTypes,
            RewardDefinitionRepository rewards,
            RewardRequestRepository rewardRequests,
            WorkspaceRepository workspaces,
            CurrentUserService currentUser,
            PointService pointService,
            ReputationService reputationService
    ) {
        this.requests = requests;
        this.taskInstances = taskInstances;
        this.pointTypes = pointTypes;
        this.rewards = rewards;
        this.rewardRequests = rewardRequests;
        this.workspaces = workspaces;
        this.currentUser = currentUser;
        this.pointService = pointService;
        this.reputationService = reputationService;
    }

    @Transactional
    public TaskRewardRequestResponse create(
            Long workspaceId,
            Long instanceId,
            TaskRewardRequestCreateRequest input
    ) {
        TaskInstance instance = getInstance(workspaceId, instanceId);
        WorkspaceMember requester = currentUser.getCurrentAccount().getWorkspaceMember();

        if (!instance.getMember().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("Only the current task executor can request a reward change");
        }

        if (instance.getStatus() == TaskInstanceStatus.COMPLETED
                || instance.getStatus() == TaskInstanceStatus.MISSED
                || instance.getStatus() == TaskInstanceStatus.EXCUSED
                || instance.getStatus() == TaskInstanceStatus.CANCELLED) {
            throw new IllegalArgumentException("Reward cannot be negotiated for a finished task");
        }

        if (requests.existsByTaskInstanceIdAndRequestedByIdAndStatusIn(
                instanceId,
                requester.getId(),
                List.of(TaskRewardRequestStatus.REQUESTED, TaskRewardRequestStatus.APPROVED)
        )) {
            throw new IllegalArgumentException("An active reward request already exists for this task execution");
        }

        validateTerms(
                workspaceId,
                input.getPointTypeId(),
                input.getPointAmount(),
                input.getReputationAmount(),
                input.getRewardDefinitionId(),
                input.getCustomRewardTitle()
        );

        TaskRewardRequest request = new TaskRewardRequest(
                getWorkspace(workspaceId),
                instance,
                requester
        );

        request.setRequestedTerms(
                getPointType(workspaceId, input.getPointTypeId(), input.getPointAmount()),
                normalizeAmount(input.getPointAmount()),
                normalizeAmount(input.getReputationAmount()),
                getReward(workspaceId, input.getRewardDefinitionId()),
                blankToNull(input.getCustomRewardTitle()),
                blankToNull(input.getComment())
        );

        return new TaskRewardRequestResponse(requests.save(request));
    }

    @Transactional(readOnly = true)
    public List<TaskRewardRequestResponse> list(
            Long workspaceId,
            Long instanceId
    ) {
        TaskInstance instance = getInstance(workspaceId, instanceId);
        WorkspaceMember member = currentUser.getCurrentAccount().getWorkspaceMember();

        if (!instance.getMember().getId().equals(member.getId()) && !canReview(member)) {
            throw new IllegalArgumentException("Task reward request access denied");
        }

        return requests.findByTaskInstanceIdOrderByCreatedAtDesc(instanceId)
                .stream()
                .map(TaskRewardRequestResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskRewardRequestResponse> listPending(Long workspaceId) {
        WorkspaceMember member = currentUser.getCurrentAccount().getWorkspaceMember();
        if (!canReview(member)) {
            throw new IllegalArgumentException("Task reward request review access denied");
        }

        return requests.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .filter(request -> request.getStatus() == TaskRewardRequestStatus.REQUESTED)
                .map(TaskRewardRequestResponse::new)
                .toList();
    }

    @Transactional
    public TaskRewardRequestResponse approve(
            Long workspaceId,
            Long requestId,
            TaskRewardRequestReviewRequest input
    ) {
        WorkspaceMember reviewer = currentUser.getCurrentAccount().getWorkspaceMember();
        if (!canReview(reviewer)) {
            throw new IllegalArgumentException("Task reward request review access denied");
        }

        TaskRewardRequest request = getRequest(workspaceId, requestId);
        if (request.getStatus() != TaskRewardRequestStatus.REQUESTED) {
            throw new IllegalArgumentException("Only requested reward changes can be approved");
        }

        validateTerms(
                workspaceId,
                input.getPointTypeId(),
                input.getPointAmount(),
                input.getReputationAmount(),
                input.getRewardDefinitionId(),
                input.getCustomRewardTitle()
        );

        request.approve(
                getPointType(workspaceId, input.getPointTypeId(), input.getPointAmount()),
                normalizeAmount(input.getPointAmount()),
                normalizeAmount(input.getReputationAmount()),
                getReward(workspaceId, input.getRewardDefinitionId()),
                blankToNull(input.getCustomRewardTitle()),
                blankToNull(input.getComment())
        );

        return new TaskRewardRequestResponse(request);
    }

    @Transactional
    public TaskRewardRequestResponse reject(
            Long workspaceId,
            Long requestId,
            String comment
    ) {
        WorkspaceMember reviewer = currentUser.getCurrentAccount().getWorkspaceMember();
        if (!canReview(reviewer)) {
            throw new IllegalArgumentException("Task reward request review access denied");
        }

        TaskRewardRequest request = getRequest(workspaceId, requestId);
        if (request.getStatus() != TaskRewardRequestStatus.REQUESTED) {
            throw new IllegalArgumentException("Only requested reward changes can be rejected");
        }

        request.reject(blankToNull(comment));
        return new TaskRewardRequestResponse(request);
    }

    @Transactional
    public TaskRewardRequestResponse cancel(
            Long workspaceId,
            Long requestId
    ) {
        TaskRewardRequest request = getRequest(workspaceId, requestId);
        Long memberId = currentUser.getCurrentMemberId();

        if (!request.getRequestedBy().getId().equals(memberId)) {
            throw new IllegalArgumentException("Only the requester can cancel this reward request");
        }
        if (request.getStatus() != TaskRewardRequestStatus.REQUESTED) {
            throw new IllegalArgumentException("Only pending requests can be cancelled");
        }

        request.cancel();
        return new TaskRewardRequestResponse(request);
    }

    /**
     * Returns true when an approved negotiation replaced the normal TaskInstance reward.
     */
    @Transactional
    public boolean processApprovedOverride(Long workspaceId, TaskInstance instance) {
        TaskRewardRequest request = requests
                .findFirstByTaskInstanceIdAndStatusOrderByCreatedAtDesc(
                        instance.getId(),
                        TaskRewardRequestStatus.APPROVED
                )
                .orElse(null);

        if (request == null) {
            return false;
        }

        Integer points = normalizeAmount(request.getApprovedPointAmount());
        if (points != null && request.getApprovedPointType() != null) {
            pointService.earn(
                    workspaceId,
                    instance.getMember().getId(),
                    request.getApprovedPointType().getId(),
                    points,
                    PointTransactionSourceType.TASK,
                    instance.getId(),
                    "Negotiated task reward: " + instance.getTitle()
            );
        }

        Integer reputation = normalizeAmount(request.getApprovedReputationAmount());
        if (reputation != null) {
            reputationService.add(
                    workspaceId,
                    instance.getMember().getId(),
                    reputation,
                    ReputationTransactionSourceType.TASK,
                    instance.getId(),
                    "Negotiated task reputation: " + instance.getTitle()
            );
        }

        createEarnedRewardRequest(workspaceId, instance, request);
        request.fulfill();
        return true;
    }

    private void createEarnedRewardRequest(
            Long workspaceId,
            TaskInstance instance,
            TaskRewardRequest negotiation
    ) {
        RewardDefinition reward = negotiation.getApprovedRewardDefinition();
        String customTitle = blankToNull(negotiation.getApprovedCustomRewardTitle());

        if (reward == null && customTitle == null) {
            return;
        }

        String title = reward != null ? reward.getTitle() : customTitle;
        PointType pointType = reward != null
                ? reward.getPointType()
                : pointTypes.findByWorkspaceIdAndCode(workspaceId, "POINTS")
                        .orElseGet(() -> instance.getRewardPointType());

        if (pointType == null) {
            throw new IllegalArgumentException("No point type available for earned reward");
        }

        RewardRequest rewardRequest = new RewardRequest(
                getWorkspace(workspaceId),
                instance.getMember(),
                reward,
                title,
                "Earned by completing task: " + instance.getTitle()
        );
        rewardRequest.approve(pointType, 0, null, false);
        rewardRequests.save(rewardRequest);
    }

    private void validateTerms(
            Long workspaceId,
            Long pointTypeId,
            Integer pointAmount,
            Integer reputationAmount,
            Long rewardDefinitionId,
            String customRewardTitle
    ) {
        if (pointAmount != null && pointAmount < 0) {
            throw new IllegalArgumentException("Point amount cannot be negative");
        }
        if (reputationAmount != null && reputationAmount < 0) {
            throw new IllegalArgumentException("Reputation amount cannot be negative");
        }
        if (pointAmount != null && pointAmount > 0 && pointTypeId == null) {
            throw new IllegalArgumentException("Point type is required when points are requested");
        }

        if (pointTypeId != null) getPointType(workspaceId, pointTypeId, 1);
        if (rewardDefinitionId != null) getReward(workspaceId, rewardDefinitionId);

        boolean hasSomething =
                (pointAmount != null && pointAmount > 0)
                || (reputationAmount != null && reputationAmount > 0)
                || rewardDefinitionId != null
                || blankToNull(customRewardTitle) != null;

        if (!hasSomething) {
            throw new IllegalArgumentException("At least one requested reward is required");
        }
    }

    private PointType getPointType(Long workspaceId, Long pointTypeId, Integer pointAmount) {
        if (pointTypeId == null || pointAmount == null || pointAmount <= 0) return null;
        return pointTypes.findByIdAndWorkspaceId(pointTypeId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Point type not found: " + pointTypeId));
    }

    private RewardDefinition getReward(Long workspaceId, Long rewardId) {
        if (rewardId == null) return null;
        return rewards.findByIdAndWorkspaceId(rewardId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found: " + rewardId));
    }

    private TaskInstance getInstance(Long workspaceId, Long instanceId) {
        return taskInstances.findByIdAndTaskDefinitionWorkspaceId(instanceId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task instance not found: " + instanceId));
    }

    private TaskRewardRequest getRequest(Long workspaceId, Long requestId) {
        return requests.findByIdAndWorkspaceId(requestId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Task reward request not found: " + requestId));
    }

    private Workspace getWorkspace(Long workspaceId) {
        return workspaces.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));
    }

    private boolean canReview(WorkspaceMember member) {
        WorkspaceRole role = member.getWorkspaceRole();
        return role.hasPermission(WorkspacePermission.MANAGE_TASKS)
                || role.hasPermission(WorkspacePermission.MANAGE_REWARDS)
                || role.hasPermission(WorkspacePermission.ADMIN_OVERRIDE);
    }

    private Integer normalizeAmount(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
