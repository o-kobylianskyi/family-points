package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.ResourceNotFoundException;
import com.olehkobylianskyi.familypoints.repository.ReputationTransactionRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReputationService {

    private final ReputationTransactionRepository transactions;
    private final WorkspaceMemberRepository members;

    public ReputationService(
            ReputationTransactionRepository transactions,
            WorkspaceMemberRepository members
    ) {
        this.transactions = transactions;
        this.members = members;
    }

    @Transactional
    public ReputationTransaction add(
            Long workspaceId,
            Long memberId,
            int amount,
            ReputationTransactionSourceType sourceType,
            Long sourceId,
            String description
    ) {
        WorkspaceMember member = members.findByIdAndWorkspaceId(memberId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found: " + memberId));

        if (amount == 0) {
            throw new IllegalArgumentException("Reputation amount cannot be zero");
        }

        return transactions.save(
                new ReputationTransaction(member, amount, sourceType, sourceId, description)
        );
    }

    @Transactional(readOnly = true)
    public long getBalance(Long memberId) {
        Long balance = transactions.getBalance(memberId);
        return balance == null ? 0L : balance;
    }
}
