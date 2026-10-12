package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.ReputationService;
import org.springframework.web.bind.annotation.*;

/** Reputation is earned, never spent, and always resolved for the active workspace member. */
@RestController
@RequestMapping("/api/me/reputation")
public class MyReputationController {
    private final CurrentUserService currentUser;
    private final ReputationService reputation;

    public MyReputationController(CurrentUserService currentUser, ReputationService reputation) {
        this.currentUser = currentUser;
        this.reputation = reputation;
    }

    public record ReputationBalance(Long workspaceId, Long memberId, long balance) {}

    @GetMapping
    public ReputationBalance balance() {
        var member = currentUser.getCurrentMember();
        return new ReputationBalance(member.getWorkspace().getId(), member.getId(),
                reputation.getBalance(member.getId()));
    }
}
