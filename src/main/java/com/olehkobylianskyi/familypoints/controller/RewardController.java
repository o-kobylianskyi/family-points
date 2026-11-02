package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.RewardService;
import com.olehkobylianskyi.familypoints.service.RewardAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;
import com.olehkobylianskyi.familypoints.entity.RewardPurchaseStatus;

@RestController
@RequestMapping("/workspaces/{workspaceId}/rewards")
public class RewardController {

    private final RewardService rewardService;
    private final RewardAvailabilityService availabilityService;
    private final CurrentUserService currentUser;

    public RewardController(
            RewardService rewardService,
            RewardAvailabilityService availabilityService,
            CurrentUserService currentUser
    ) {
        this.rewardService = rewardService;
        this.availabilityService = availabilityService;
        this.currentUser = currentUser;
    }

    @GetMapping("/categories")
    public List<RewardCategoryResponse> categories(
            @PathVariable Long workspaceId
    ) {
        assertWorkspace(workspaceId);
        return rewardService.listCategories(workspaceId);
    }

    @PostMapping("/categories")
    public RewardCategoryResponse createCategory(
            @PathVariable Long workspaceId,
            @Valid @RequestBody RewardCategoryRequest request
    ) {
        assertManage(workspaceId);
        return rewardService.createCategory(workspaceId, request);
    }

    @GetMapping
    public List<RewardDefinitionResponse> rewards(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        assertWorkspace(workspaceId);
        return rewardService.listRewards(
                workspaceId,
                includeInactive && canManageRewards()
        );
    }

    @PostMapping
    public RewardDefinitionResponse createReward(
            @PathVariable Long workspaceId,
            @Valid @RequestBody RewardDefinitionRequest request
    ) {
        assertManage(workspaceId);
        return rewardService.createReward(workspaceId, request);
    }

    @PutMapping("/{rewardId}")
    public RewardDefinitionResponse updateReward(
            @PathVariable Long workspaceId,
            @PathVariable Long rewardId,
            @Valid @RequestBody RewardDefinitionRequest request
    ) {
        assertManage(workspaceId);
        return rewardService.updateReward(workspaceId, rewardId, request);
    }

    @GetMapping("/{rewardId}/availability")
    public RewardAvailabilityResponse availability(@PathVariable Long workspaceId, @PathVariable Long rewardId) {
        assertWorkspace(workspaceId);
        return availabilityService.get(workspaceId, rewardId, canManageRewards());
    }

    @PutMapping("/{rewardId}/availability")
    public RewardAvailabilityResponse updateAvailability(@PathVariable Long workspaceId,
                                                          @PathVariable Long rewardId,
                                                          @Valid @RequestBody RewardAvailabilityRequest request) {
        assertManage(workspaceId);
        return availabilityService.update(workspaceId, rewardId, request);
    }

    @GetMapping("/requests")
    public List<RewardRequestResponse> requests(
            @PathVariable Long workspaceId
    ) {
        assertWorkspace(workspaceId);
        return rewardService.listRequests(
                workspaceId,
                currentUser.getCurrentMemberId(),
                canManageRewards()
        );
    }

    @PostMapping("/requests")
    public RewardRequestResponse createRequest(
            @PathVariable Long workspaceId,
            @Valid @RequestBody RewardRequestCreateRequest request
    ) {
        assertWorkspace(workspaceId);
        return rewardService.createRequest(
                workspaceId,
                currentUser.getCurrentMemberId(),
                request
        );
    }

    @PostMapping("/requests/{requestId}/approve")
    public RewardRequestResponse approveRequest(
            @PathVariable Long workspaceId,
            @PathVariable Long requestId,
            @Valid @RequestBody RewardRequestReviewRequest request
    ) {
        assertManage(workspaceId);
        return rewardService.approveRequest(workspaceId, requestId, request);
    }

    @PostMapping("/requests/{requestId}/reject")
    public RewardRequestResponse rejectRequest(
            @PathVariable Long workspaceId,
            @PathVariable Long requestId
    ) {
        assertManage(workspaceId);
        return rewardService.rejectRequest(workspaceId, requestId);
    }

    @PostMapping("/requests/{requestId}/refresh")
    public RewardRequestResponse refreshRequest(
            @PathVariable Long workspaceId,
            @PathVariable Long requestId
    ) {
        assertWorkspace(workspaceId);
        return rewardService.refreshRequest(workspaceId, requestId);
    }

    /**
     * Administrator performs a real purchase for a child. The reward service
     * checks eligibility, approval conditions and balance, then creates both
     * the purchase and linked REWARD_PURCHASE point transaction atomically.
     */
    @PostMapping("/{rewardId}/purchase-for-member/{memberId}")
    public RewardPurchaseResponse purchaseRewardForMember(
            @PathVariable Long workspaceId,
            @PathVariable Long rewardId,
            @PathVariable Long memberId
    ) {
        assertManage(workspaceId);
        return rewardService.purchaseReward(workspaceId, memberId, rewardId);
    }

    @PostMapping("/{rewardId}/purchase")
    public RewardPurchaseResponse purchaseReward(
            @PathVariable Long workspaceId,
            @PathVariable Long rewardId
    ) {
        assertWorkspace(workspaceId);
        return rewardService.purchaseReward(
                workspaceId,
                currentUser.getCurrentMemberId(),
                rewardId
        );
    }

    @PostMapping("/requests/{requestId}/purchase")
    public RewardPurchaseResponse purchaseRequest(
            @PathVariable Long workspaceId,
            @PathVariable Long requestId
    ) {
        assertWorkspace(workspaceId);
        return rewardService.purchaseRequest(
                workspaceId,
                currentUser.getCurrentMemberId(),
                requestId
        );
    }

    @GetMapping("/obligations/open")
    public List<RewardObligationResponse> openObligations(
            @PathVariable Long workspaceId
    ) {
        assertWorkspace(workspaceId);
        return rewardService.listOpenObligations(
                workspaceId,
                currentUser.getCurrentMemberId(),
                canManageRewards()
        );
    }

    /**
     * Purchase history only; no delivery/receipt event is inferred from READY
     * or REDEEMED. All member filters are server-authorized.
     */
    @GetMapping("/history")
    public RewardPurchaseHistoryPage history(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) RewardPurchaseStatus status,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to
    ) {
        assertWorkspace(workspaceId);
        return rewardService.purchaseHistory(workspaceId,
                currentUser.getCurrentMemberId(),canManageRewards(),
                memberId,limit,cursor,status,from,to);
    }

    @GetMapping("/purchases")
    public List<RewardPurchaseResponse> purchases(
            @PathVariable Long workspaceId
    ) {
        assertWorkspace(workspaceId);
        return rewardService.listPurchases(
                workspaceId,
                currentUser.getCurrentMemberId(),
                canManageRewards()
        );
    }

    private void assertWorkspace(Long workspaceId) {
        if (!currentUser.belongsToWorkspace(workspaceId)) {
            throw new IllegalArgumentException("Workspace access denied");
        }
    }

    private void assertManage(Long workspaceId) {
        assertWorkspace(workspaceId);
        if (!canManageRewards()) {
            throw new IllegalArgumentException("MANAGE_REWARDS permission required");
        }
    }

    private boolean canManageRewards() {
        var role = currentUser.getCurrentMember().getWorkspaceRole();

        return role.hasPermission(WorkspacePermission.MANAGE_REWARDS)
                || role.hasPermission(WorkspacePermission.ADMIN_OVERRIDE);
    }
}
