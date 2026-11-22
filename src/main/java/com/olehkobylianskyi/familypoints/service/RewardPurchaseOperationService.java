package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.RewardPurchaseResponse;
import com.olehkobylianskyi.familypoints.entity.WorkspacePermission;
import com.olehkobylianskyi.familypoints.repository.RewardPurchaseRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * One reward purchase intent -> one purchase, one authoritative REWARD_PURCHASE debit.
 *
 * The same family FOR UPDATE lock guards epoch validation, receipt lookup, purchase
 * and receipt INSERT. The existing RewardService purchase transaction joins this
 * transaction; failed purchase or receipt persistence rolls back all three.
 * No automatic client retry is authorized merely by this source implementation.
 */
@Service
public class RewardPurchaseOperationService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epochs;
    private final CurrentUserService currentUser;
    private final RewardService rewards;
    private final RewardPurchaseRepository purchases;

    public RewardPurchaseOperationService(JdbcTemplate db,
            WorkspaceEpochTransactionGuard epochs, CurrentUserService currentUser,
            RewardService rewards, RewardPurchaseRepository purchases) {
        this.db = db;
        this.epochs = epochs;
        this.currentUser = currentUser;
        this.rewards = rewards;
        this.purchases = purchases;
    }

    public record Receipt(UUID operationId, RewardPurchaseResponse purchase, boolean replayed) {}
    private record Stored(String fingerprint, long epoch, String route, long memberId,
                          long rewardId, long purchaseId) {}

    @Transactional
    public Receipt purchaseSelf(long workspaceId, long rewardId, UUID operationId) {
        requireWorkspace(workspaceId);
        long memberId = currentUser.getCurrentMemberId();
        return purchase(workspaceId, memberId, rewardId, operationId, "SELF");
    }

    @Transactional
    public Receipt purchaseForMember(long workspaceId, long memberId, long rewardId, UUID operationId) {
        requireWorkspace(workspaceId);
        requireManageRewards();
        return purchase(workspaceId, memberId, rewardId, operationId, "MANAGER");
    }

    private Receipt purchase(long workspaceId, long memberId, long rewardId,
                             UUID operationId, String route) {
        requireValid(operationId, workspaceId, memberId, rewardId);
        long actor = currentUser.getCurrentAccount().getId();
        // Transaction-scoped lock serializes same-family duplicate calls and
        // fences Reset/dataEpoch before any receipt lookup or wallet mutation.
        long epoch = epochs.requireCurrentRequestEpoch(workspaceId);
        String fingerprint = fingerprint(route, memberId, rewardId, epoch);
        Stored existing = load(workspaceId, actor, operationId);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)
                    || existing.epoch() != epoch
                    || !existing.route().equals(route)
                    || existing.memberId() != memberId
                    || existing.rewardId() != rewardId) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "REWARD_PURCHASE_OPERATION_CONFLICT");
            }
            return new Receipt(operationId,
                    validatedPurchase(workspaceId, existing.memberId(),
                            existing.rewardId(), existing.purchaseId()), true);
        }

        // This method enforces authoritative cost/type, availability, limits,
        // balance and reward/member tenancy. Never call generic spend again.
        RewardPurchaseResponse purchase = rewards.purchaseReward(workspaceId, memberId, rewardId);
        db.update("""
            INSERT INTO reward_purchase_operation_receipts
              (workspace_id,actor_account_id,operation_id,request_fingerprint,
               request_epoch,route_kind,member_id,reward_definition_id,purchase_id)
            VALUES (?,?,?,?,?,?,?,?,?)
            """, workspaceId, actor, operationId, fingerprint, epoch, route,
                memberId, rewardId, purchase.getId());
        return new Receipt(operationId, purchase, false);
    }

    @Transactional(readOnly = true)
    public Receipt receipt(long workspaceId, UUID operationId) {
        requireWorkspace(workspaceId);
        if (operationId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "REWARD_PURCHASE_OPERATION_ID_REQUIRED");
        long actor = currentUser.getCurrentAccount().getId();
        Stored stored = load(workspaceId, actor, operationId);
        if (stored == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "REWARD_PURCHASE_RECEIPT_NOT_FOUND");
        if ("MANAGER".equals(stored.route())) requireManageRewards();
        else if (!currentUser.getCurrentMemberId().equals(stored.memberId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "REWARD_PURCHASE_RECEIPT_MEMBER_DENIED");
        return new Receipt(operationId,
                validatedPurchase(workspaceId, stored.memberId(),
                        stored.rewardId(), stored.purchaseId()), true);
    }

    private RewardPurchaseResponse validatedPurchase(long workspaceId, long memberId,
                                                       long rewardId, long purchaseId) {
        var purchase = purchases.findById(purchaseId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "REWARD_PURCHASE_RECEIPT_BROKEN"));
        if (!purchase.getMember().getWorkspace().getId().equals(workspaceId)
                || !purchase.getMember().getId().equals(memberId)
                || purchase.getRewardDefinition() == null
                || !purchase.getRewardDefinition().getId().equals(rewardId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "REWARD_PURCHASE_RECEIPT_BROKEN");
        }
        return RewardPurchaseResponse.from(purchase);
    }

    private Stored load(long workspaceId, long actor, UUID operationId) {
        List<Stored> rows = db.query("""
            SELECT request_fingerprint,request_epoch,route_kind,member_id,
                   reward_definition_id,purchase_id
            FROM reward_purchase_operation_receipts
            WHERE workspace_id=? AND actor_account_id=? AND operation_id=?
            """, (rs, row) -> new Stored(rs.getString(1), rs.getLong(2),
                rs.getString(3), rs.getLong(4), rs.getLong(5), rs.getLong(6)),
                workspaceId, actor, operationId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void requireWorkspace(long workspaceId) {
        if (workspaceId <= 0 || !currentUser.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "WORKSPACE_ACCESS_DENIED");
    }

    private void requireManageRewards() {
        var role = currentUser.getCurrentMember().getWorkspaceRole();
        if (!role.hasPermission(WorkspacePermission.MANAGE_REWARDS)
                && !role.hasPermission(WorkspacePermission.ADMIN_OVERRIDE))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MANAGE_REWARDS_REQUIRED");
    }

    private static void requireValid(UUID key, long workspace, long member, long reward) {
        if (key == null || workspace <= 0 || member <= 0 || reward <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "REWARD_PURCHASE_OPERATION_INVALID");
    }

    static String fingerprint(String route, long member, long reward, long epoch) {
        try {
            String canonical = route + "\u0000" + member + "\u0000" + reward + "\u0000" + epoch;
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("REWARD_PURCHASE_FINGERPRINT_FAILURE", e);
        }
    }
}
