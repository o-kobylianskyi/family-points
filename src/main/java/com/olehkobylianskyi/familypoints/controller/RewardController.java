package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces/{workspaceId}/rewards")
public class RewardController {

    private final RewardService rewardService;
    private final CurrentUserService currentUser;

    public RewardController(
            RewardService rewardService,
            CurrentUserService currentUser
    ) {
        this.rewardService = rewardService;
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
        var role = currentUser.getCurrentAccount()
                .getWorkspaceMember()
                .getWorkspaceRole();

        return role.hasPermission(WorkspacePermission.MANAGE_REWARDS)
                || role.hasPermission(WorkspacePermission.ADMIN_OVERRIDE);
    }
}
