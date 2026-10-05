package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RewardService {

    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final PointTypeRepository pointTypes;
    private final RewardCategoryRepository categories;
    private final RewardDefinitionRepository rewards;
    private final RewardRequestRepository requests;
    private final RewardRequirementRepository requirements;
    private final RewardPurchaseRepository purchases;
    private final RewardObligationRepository obligations;
    private final TaskDefinitionRepository taskDefinitions;
    private final TaskInstanceRepository taskInstances;
    private final ReputationService reputationService;
    private final PointService pointService;

    public RewardService(
            WorkspaceRepository workspaces,
            WorkspaceMemberRepository members,
            PointTypeRepository pointTypes,
            RewardCategoryRepository categories,
            RewardDefinitionRepository rewards,
            RewardRequestRepository requests,
            RewardRequirementRepository requirements,
            RewardPurchaseRepository purchases,
            RewardObligationRepository obligations,
            TaskDefinitionRepository taskDefinitions,
            TaskInstanceRepository taskInstances,
            ReputationService reputationService,
            PointService pointService
    ) {
        this.workspaces = workspaces;
        this.members = members;
        this.pointTypes = pointTypes;
        this.categories = categories;
        this.rewards = rewards;
        this.requests = requests;
        this.requirements = requirements;
        this.purchases = purchases;
        this.obligations = obligations;
        this.taskDefinitions = taskDefinitions;
        this.taskInstances = taskInstances;
        this.reputationService = reputationService;
        this.pointService = pointService;
    }

    @Transactional(readOnly = true)
    public List<RewardCategoryResponse> listCategories(Long workspaceId) {
        return categories.findByWorkspaceIdAndActiveTrueOrderBySortOrderAscIdAsc(workspaceId)
                .stream()
                .map(RewardCategoryResponse::new)
                .toList();
    }

    @Transactional
    public RewardCategoryResponse createCategory(Long workspaceId, RewardCategoryRequest request) {
        Workspace workspace = getWorkspace(workspaceId);
        RewardCategory saved = categories.save(
                new RewardCategory(workspace, request.getName().trim(), request.getSortOrder())
        );
        return new RewardCategoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RewardDefinitionResponse> listRewards(Long workspaceId, boolean includeInactive) {
        List<RewardDefinition> list = includeInactive
                ? rewards.findByWorkspaceIdOrderByIdAsc(workspaceId)
                : rewards.findByWorkspaceIdAndActiveTrueOrderByIdAsc(workspaceId);

        return list.stream()
                .map(reward -> RewardDefinitionResponse.from(
                        reward,
                        requirements.findByRewardDefinitionIdOrderBySortOrderAscIdAsc(reward.getId())
                                .stream()
                                .map(RewardRequirementResponse::from)
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public RewardDefinitionResponse createReward(Long workspaceId, RewardDefinitionRequest request) {
        Workspace workspace = getWorkspace(workspaceId);
        PointType pointType = getPointType(workspaceId, request.getPointTypeId());

        if (request.getRewardKind() == RewardKind.STANDARD && request.getDefaultDurationMinutes() != null) {
            throw new IllegalArgumentException("Standard reward cannot have duration");
        }

        RewardDefinition reward = new RewardDefinition(
                workspace,
                request.getTitle().trim(),
                blankToNull(request.getDescription()),
                pointType,
                request.getPriceAmount(),
                request.getMinimumReputation(),
                request.isRequiresApproval(),
                request.getAcquisitionMode(),
                request.getRewardKind(),
                request.getRewardKind() == RewardKind.TIME_BASED
                        ? request.getDefaultDurationMinutes()
                        : null
        );

        reward.setCategories(resolveCategories(workspaceId, request.getCategoryIds()));
        RewardDefinition saved = rewards.save(reward);
        saveRequirements(workspaceId, saved, null, request.getRequirements());

        return RewardDefinitionResponse.from(
                saved,
                requirements.findByRewardDefinitionIdOrderBySortOrderAscIdAsc(saved.getId())
                        .stream()
                        .map(RewardRequirementResponse::from)
                        .toList()
        );
    }

    @Transactional
    public RewardRequestResponse createRequest(
            Long workspaceId,
            Long memberId,
            RewardRequestCreateRequest request
    ) {
        Workspace workspace = getWorkspace(workspaceId);
        WorkspaceMember member = getMember(workspaceId, memberId);

        RewardDefinition reward = null;
        String title = request.getTitle();
        String description = request.getDescription();

        if (request.getRewardDefinitionId() != null) {
            reward = rewards.findByIdAndWorkspaceId(request.getRewardDefinitionId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reward not found: " + request.getRewardDefinitionId()));
            if (title == null || title.isBlank()) title = reward.getTitle();
            if (description == null || description.isBlank()) description = reward.getDescription();
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Reward request title is required");
        }

        RewardRequest saved = requests.save(
                new RewardRequest(workspace, member, reward, title.trim(), blankToNull(description))
        );

        return toRequestResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RewardRequestResponse> listRequests(
            Long workspaceId,
            Long currentMemberId,
            boolean canManage
    ) {
        List<RewardRequest> list = canManage
                ? requests.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                : requests.findByRequestedByIdOrderByCreatedAtDesc(currentMemberId);

        return list.stream().map(this::toRequestResponse).toList();
    }

    @Transactional
    public RewardRequestResponse approveRequest(
            Long workspaceId,
            Long requestId,
            RewardRequestReviewRequest review
    ) {
        RewardRequest rewardRequest = getRequest(workspaceId, requestId);

        Long pointTypeId = review.getPointTypeId();
        Integer price = review.getPriceAmount();

        if (rewardRequest.getRewardDefinition() != null) {
            if (pointTypeId == null) pointTypeId = rewardRequest.getRewardDefinition().getPointType().getId();
            if (price == null) price = rewardRequest.getRewardDefinition().getPriceAmount();
        }

        if (pointTypeId == null) {
            throw new IllegalArgumentException("Point type is required");
        }
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Reward price must be zero or greater");
        }

        PointType pointType = getPointType(workspaceId, pointTypeId);

        requirements.deleteByRewardRequestId(rewardRequest.getId());
        saveRequirements(workspaceId, null, rewardRequest, review.getRequirements());

        boolean hasBefore = requirements
                .findByRewardRequestIdAndPhaseOrderBySortOrderAscIdAsc(
                        rewardRequest.getId(),
                        RewardRequirementPhase.BEFORE_REWARD
                )
                .stream()
                .anyMatch(RewardRequirement::isRequired);

        rewardRequest.approve(
                pointType,
                price,
                review.getMinimumReputation(),
                hasBefore
        );

        refreshRequestReadiness(rewardRequest);
        return toRequestResponse(rewardRequest);
    }

    @Transactional
    public RewardRequestResponse rejectRequest(Long workspaceId, Long requestId) {
        RewardRequest request = getRequest(workspaceId, requestId);
        request.reject();
        return toRequestResponse(request);
    }

    @Transactional
    public RewardPurchaseResponse purchaseReward(
            Long workspaceId,
            Long memberId,
            Long rewardDefinitionId
    ) {
        WorkspaceMember member = getMember(workspaceId, memberId);
        RewardDefinition reward = rewards.findByIdAndWorkspaceId(rewardDefinitionId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found: " + rewardDefinitionId));

        if (!reward.isActive()) {
            throw new IllegalArgumentException("Reward is inactive");
        }
        if (reward.getAcquisitionMode() == RewardAcquisitionMode.REQUEST || reward.isRequiresApproval()) {
            throw new IllegalArgumentException("This reward requires a request and approval");
        }

        assertNotBlocked(member, reward);

        if (reward.getMinimumReputation() != null
                && reputationService.getBalance(memberId) < reward.getMinimumReputation()) {
            throw new IllegalArgumentException("Minimum reputation requirement is not met");
        }

        List<RewardRequirement> before = requirements
                .findByRewardDefinitionIdAndPhaseOrderBySortOrderAscIdAsc(
                        reward.getId(),
                        RewardRequirementPhase.BEFORE_REWARD
                );

        if (!allRequirementsMet(member, before, null)) {
            throw new IllegalArgumentException("Reward requirements are not met");
        }

        RewardPurchase purchase = purchases.save(
                new RewardPurchase(reward, member, RewardPurchaseStatus.READY)
        );

        if (reward.getPriceAmount() > 0) {
            pointService.spend(
                    workspaceId,
                    memberId,
                    reward.getPointType().getId(),
                    reward.getPriceAmount(),
                    PointTransactionSourceType.REWARD_PURCHASE,
                    purchase.getId(),
                    "Reward: " + reward.getTitle()
            );
        }

        createPostObligations(
                workspaceId,
                member,
                purchase,
                requirements.findByRewardDefinitionIdAndPhaseOrderBySortOrderAscIdAsc(
                        reward.getId(),
                        RewardRequirementPhase.AFTER_REWARD
                )
        );

        return RewardPurchaseResponse.from(purchase);
    }

    @Transactional
    public RewardPurchaseResponse purchaseRequest(
            Long workspaceId,
            Long memberId,
            Long requestId
    ) {
        WorkspaceMember member = getMember(workspaceId, memberId);
        RewardRequest request = getRequest(workspaceId, requestId);

        if (!request.getRequestedBy().getId().equals(memberId)) {
            throw new IllegalArgumentException("Reward request belongs to another member");
        }

        refreshRequestReadiness(request);

        if (request.getStatus() != RewardRequestStatus.READY_TO_PURCHASE) {
            throw new IllegalArgumentException("Reward request is not ready to purchase");
        }

        if (request.getMinimumReputation() != null
                && reputationService.getBalance(memberId) < request.getMinimumReputation()) {
            throw new IllegalArgumentException("Minimum reputation requirement is not met");
        }

        if (request.getRewardDefinition() != null) {
            assertNotBlocked(member, request.getRewardDefinition());
        } else {
            assertNoGlobalBlock(member);
        }

        RewardPurchase purchase = purchases.save(
                new RewardPurchase(request, member, RewardPurchaseStatus.READY)
        );

        if (request.getApprovedPriceAmount() != null && request.getApprovedPriceAmount() > 0) {
            pointService.spend(
                    workspaceId,
                    memberId,
                    request.getApprovedPointType().getId(),
                    request.getApprovedPriceAmount(),
                    PointTransactionSourceType.REWARD_PURCHASE,
                    purchase.getId(),
                    "Reward request: " + request.getTitle()
            );
        }

        createPostObligations(
                workspaceId,
                member,
                purchase,
                requirements.findByRewardRequestIdAndPhaseOrderBySortOrderAscIdAsc(
                        request.getId(),
                        RewardRequirementPhase.AFTER_REWARD
                )
        );

        request.markPurchased();
        return RewardPurchaseResponse.from(purchase);
    }

    @Transactional(readOnly = true)
    public List<RewardPurchaseResponse> listPurchases(Long workspaceId, Long currentMemberId, boolean canManage) {
        return (canManage
                ? purchases.findByMemberWorkspaceIdOrderByPurchasedAtDesc(workspaceId)
                : purchases.findByMemberIdOrderByPurchasedAtDesc(currentMemberId))
                .stream()
                .map(RewardPurchaseResponse::from)
                .toList();
    }

    @Transactional
    public List<RewardObligationResponse> listOpenObligations(
            Long workspaceId,
            Long currentMemberId,
            boolean canManage
    ) {
        if (canManage) {
            List<RewardObligation> open = obligations
                    .findByMemberWorkspaceIdAndStatusOrderByCreatedAtDesc(
                            workspaceId,
                            RewardObligationStatus.OPEN
                    );

            open.forEach(obligation -> refreshObligation(obligation));
            return open.stream()
                    .filter(obligation -> obligation.getStatus() == RewardObligationStatus.OPEN)
                    .map(RewardObligationResponse::new)
                    .toList();
        }

        WorkspaceMember member = getMember(workspaceId, currentMemberId);
        List<RewardObligation> open = refreshOpenObligations(member);
        return open.stream()
                .map(RewardObligationResponse::new)
                .toList();
    }

    @Transactional
    public RewardRequestResponse refreshRequest(Long workspaceId, Long requestId) {
        RewardRequest request = getRequest(workspaceId, requestId);
        refreshRequestReadiness(request);
        return toRequestResponse(request);
    }

    private void refreshRequestReadiness(RewardRequest request) {
        if (request.getStatus() != RewardRequestStatus.WAITING_REQUIREMENTS
                && request.getStatus() != RewardRequestStatus.APPROVED) {
            return;
        }

        List<RewardRequirement> before = requirements
                .findByRewardRequestIdAndPhaseOrderBySortOrderAscIdAsc(
                        request.getId(),
                        RewardRequirementPhase.BEFORE_REWARD
                );

        if (allRequirementsMet(request.getRequestedBy(), before, request)) {
            request.markReady();
        }
    }

    private boolean allRequirementsMet(
            WorkspaceMember member,
            List<RewardRequirement> list,
            RewardRequest request
    ) {
        return list.stream()
                .filter(RewardRequirement::isRequired)
                .allMatch(requirement -> requirementMet(member, requirement, request));
    }

    private boolean requirementMet(
            WorkspaceMember member,
            RewardRequirement requirement,
            RewardRequest request
    ) {
        if (requirement.getRequirementType() == RewardRequirementType.TASK_COMPLETED) {
            if (requirement.getTaskDefinition() == null) return false;

            if (requirement.getTimeScope() == null
                    || requirement.getTimeScope() == RewardRequirementTimeScope.TODAY) {
                return taskInstances.existsByTaskDefinitionIdAndMemberIdAndScheduledDateAndStatus(
                        requirement.getTaskDefinition().getId(),
                        member.getId(),
                        LocalDate.now(),
                        TaskInstanceStatus.COMPLETED
                );
            }

            // Other time windows are deliberately reserved by the model but not evaluated in v1 yet.
            return false;
        }

        // Behavior engine and generic custom-condition evaluator are separate next slices.
        return false;
    }

    private void createPostObligations(
            Long workspaceId,
            WorkspaceMember member,
            RewardPurchase purchase,
            List<RewardRequirement> postRequirements
    ) {
        Workspace workspace = getWorkspace(workspaceId);

        for (RewardRequirement requirement : postRequirements) {
            String title = requirement.getDescription();
            if ((title == null || title.isBlank()) && requirement.getTaskDefinition() != null) {
                title = requirement.getTaskDefinition().getTitle();
            }
            if (title == null || title.isBlank()) {
                title = requirement.getRequirementType().name();
            }

            RewardObligation obligation = new RewardObligation(
                    workspace,
                    member,
                    purchase,
                    requirement,
                    title,
                    requirement.getBlockingMode(),
                    null
            );

            obligation.getBlockedCategories().addAll(requirement.getBlockedCategories());
            obligation.getBlockedRewards().addAll(requirement.getBlockedRewards());
            obligations.save(obligation);
        }
    }

    private void assertNotBlocked(WorkspaceMember member, RewardDefinition reward) {
        List<RewardObligation> open = refreshOpenObligations(member);

        for (RewardObligation obligation : open) {
            switch (obligation.getBlockingMode()) {
                case ALL_REWARDS -> throw new IllegalArgumentException(
                        "New rewards are blocked by unfinished obligation: " + obligation.getTitle()
                );
                case CATEGORIES -> {
                    boolean blocked = reward.getCategories().stream()
                            .anyMatch(obligation.getBlockedCategories()::contains);
                    if (blocked) {
                        throw new IllegalArgumentException(
                                "This reward category is blocked by unfinished obligation: " + obligation.getTitle()
                        );
                    }
                }
                case SPECIFIC_REWARDS -> {
                    if (obligation.getBlockedRewards().contains(reward)) {
                        throw new IllegalArgumentException(
                                "This reward is blocked by unfinished obligation: " + obligation.getTitle()
                        );
                    }
                }
                default -> {
                    // NONE and WARN_ONLY do not block.
                }
            }
        }
    }

    private void assertNoGlobalBlock(WorkspaceMember member) {
        boolean blocked = refreshOpenObligations(member)
                .stream()
                .anyMatch(o -> o.getBlockingMode() == RewardBlockingMode.ALL_REWARDS);

        if (blocked) {
            throw new IllegalArgumentException("New rewards are blocked by an unfinished obligation");
        }
    }

    private List<RewardObligation> refreshOpenObligations(WorkspaceMember member) {
        List<RewardObligation> open = obligations.findByMemberIdAndStatusOrderByCreatedAtAsc(
                member.getId(),
                RewardObligationStatus.OPEN
        );

        open.forEach(this::refreshObligation);

        return open.stream()
                .filter(obligation -> obligation.getStatus() == RewardObligationStatus.OPEN)
                .toList();
    }

    private void refreshObligation(RewardObligation obligation) {
        RewardRequirement requirement = obligation.getRequirement();
        if (requirement == null) return;

        if (requirement.getRequirementType() == RewardRequirementType.TASK_COMPLETED
                && requirement.getTaskDefinition() != null
                && requirement.getTimeScope() == RewardRequirementTimeScope.SINCE_REWARD) {
            boolean completedAfterReward =
                    taskInstances.existsByTaskDefinitionIdAndMemberIdAndStatusAndCompletedAtAfter(
                            requirement.getTaskDefinition().getId(),
                            obligation.getMember().getId(),
                            TaskInstanceStatus.COMPLETED,
                            obligation.getCreatedAt()
                    );

            if (completedAfterReward) {
                obligation.complete();
            }
            return;
        }

        if (requirementMet(
                obligation.getMember(),
                requirement,
                requirement.getRewardRequest()
        )) {
            obligation.complete();
        }
    }

    private void saveRequirements(
            Long workspaceId,
            RewardDefinition reward,
            RewardRequest request,
            List<RewardRequirementRequest> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) return;

        Workspace workspace = getWorkspace(workspaceId);

        for (int i = 0; i < inputs.size(); i++) {
            RewardRequirementRequest input = inputs.get(i);
            TaskDefinition taskDefinition = null;

            if (input.getTaskDefinitionId() != null) {
                taskDefinition = taskDefinitions.findByIdAndWorkspaceId(input.getTaskDefinitionId(), workspaceId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Task definition not found: " + input.getTaskDefinitionId()
                        ));
            }

            RewardRequirement entity = new RewardRequirement(
                    workspace,
                    reward,
                    request,
                    input.getPhase(),
                    input.getRequirementType(),
                    taskDefinition,
                    input.getTimeScope(),
                    input.getWindowValue(),
                    blankToNull(input.getDescription()),
                    input.isRequired(),
                    input.getBlockingMode(),
                    i
            );

            if (input.getBlockedCategoryIds() != null) {
                entity.getBlockedCategories().addAll(
                        resolveCategories(workspaceId, input.getBlockedCategoryIds())
                );
            }

            if (input.getBlockedRewardDefinitionIds() != null) {
                for (Long rewardId : input.getBlockedRewardDefinitionIds()) {
                    entity.getBlockedRewards().add(
                            rewards.findByIdAndWorkspaceId(rewardId, workspaceId)
                                    .orElseThrow(() -> new ResourceNotFoundException(
                                            "Reward not found: " + rewardId
                                    ))
                    );
                }
            }

            requirements.save(entity);
        }
    }

    private Set<RewardCategory> resolveCategories(Long workspaceId, List<Long> ids) {
        Set<RewardCategory> result = new LinkedHashSet<>();
        if (ids == null) return result;

        for (Long id : ids) {
            result.add(categories.findByIdAndWorkspaceId(id, workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reward category not found: " + id)));
        }
        return result;
    }

    private RewardRequestResponse toRequestResponse(RewardRequest request) {
        List<RewardRequirementResponse> reqs = requirements
                .findByRewardRequestIdOrderBySortOrderAscIdAsc(request.getId())
                .stream()
                .map(RewardRequirementResponse::from)
                .toList();
        return new RewardRequestResponse(request, reqs);
    }

    private Workspace getWorkspace(Long workspaceId) {
        return workspaces.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));
    }

    private WorkspaceMember getMember(Long workspaceId, Long memberId) {
        return members.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found: " + memberId));
    }

    private PointType getPointType(Long workspaceId, Long pointTypeId) {
        return pointTypes.findByIdAndWorkspaceId(pointTypeId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Point type not found: " + pointTypeId));
    }

    private RewardRequest getRequest(Long workspaceId, Long requestId) {
        return requests.findByIdAndWorkspaceId(requestId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward request not found: " + requestId));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
