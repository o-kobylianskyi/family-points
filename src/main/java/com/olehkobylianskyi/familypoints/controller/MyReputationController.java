package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.ReputationService;
import org.springframework.web.bind.annotation.*;
import com.olehkobylianskyi.familypoints.repository.ReputationTransactionRepository;
import java.time.LocalDateTime;
import java.util.List;

/** Reputation is earned, never spent, and always resolved for the active workspace member. */
@RestController
@RequestMapping("/api/me/reputation")
public class MyReputationController {
    private final CurrentUserService currentUser;
    private final ReputationService reputation;
    private final ReputationTransactionRepository transactions;

    public MyReputationController(CurrentUserService currentUser, ReputationService reputation,
                                  ReputationTransactionRepository transactions) {
        this.currentUser = currentUser;
        this.reputation = reputation;
        this.transactions = transactions;
    }

    public record ReputationBalance(Long workspaceId, Long memberId, long balance) {}

    public record ReputationEntry(Long id, int amount, String sourceType,
                                  String description, LocalDateTime createdAt) {}

    @GetMapping("/history")
    public List<ReputationEntry> history() {
        Long memberId = currentUser.getCurrentMemberId();
        return transactions.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .limit(100)
                .map(entry -> new ReputationEntry(entry.getId(), entry.getAmount(),
                        entry.getSourceType().name(), entry.getDescription(), entry.getCreatedAt()))
                .toList();
    }

    @GetMapping
    public ReputationBalance balance() {
        var member = currentUser.getCurrentMember();
        return new ReputationBalance(member.getWorkspace().getId(), member.getId(),
                reputation.getBalance(member.getId()));
    }
}
