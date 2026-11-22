package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.RewardPurchaseResponse;
import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.RewardPurchaseRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RewardPurchaseOperationServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final WorkspaceEpochTransactionGuard epochs = mock(WorkspaceEpochTransactionGuard.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final RewardService rewards = mock(RewardService.class);
    private final RewardPurchaseRepository purchases = mock(RewardPurchaseRepository.class);
    private final RewardPurchaseOperationService service =
            new RewardPurchaseOperationService(db, epochs, current, rewards, purchases);

    private void authenticatedSelf() {
        var account = mock(UserAccount.class);
        when(account.getId()).thenReturn(22L);
        when(current.getCurrentAccount()).thenReturn(account);
        when(current.belongsToWorkspace(1L)).thenReturn(true);
        when(current.getCurrentMemberId()).thenReturn(3L);
        when(epochs.requireCurrentRequestEpoch(1L)).thenReturn(0L);
    }

    @Test
    void createsExactlyOneAuthoritativePurchaseAndReceiptInNewIntent() {
        authenticatedSelf();
        UUID key = UUID.randomUUID();
        RewardPurchaseResponse response = mock(RewardPurchaseResponse.class);
        when(response.getId()).thenReturn(77L);
        when(rewards.purchaseReward(1L, 3L, 5L)).thenReturn(response);
        when(db.query(contains("FROM reward_purchase_operation_receipts"),
                any(RowMapper.class), eq(1L), eq(22L), eq(key))).thenReturn(List.of());

        var receipt = service.purchaseSelf(1L, 5L, key);
        assertEquals(key, receipt.operationId());
        assertSame(response, receipt.purchase());
        assertFalse(receipt.replayed());
        verify(rewards, times(1)).purchaseReward(1L, 3L, 5L);
        verify(db, times(1)).update(contains("INSERT INTO reward_purchase_operation_receipts"),
                eq(1L), eq(22L), eq(key), anyString(), eq(0L),
                eq("SELF"), eq(3L), eq(5L), eq(77L));
    }

    @Test
    void invalidIntentCannotCreatePurchaseOrReceipt() {
        authenticatedSelf();
        var bad = assertThrows(ResponseStatusException.class,
                () -> service.purchaseSelf(1L, 5L, null));
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());
        verifyNoInteractions(rewards, purchases);
        verify(db, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void sameIntentFingerprintIsStableButChangesForMemberRewardRouteOrEpoch() {
        var baseline = RewardPurchaseOperationService.fingerprint("SELF", 3L, 5L, 0L);
        assertEquals(baseline, RewardPurchaseOperationService.fingerprint("SELF", 3L, 5L, 0L));
        assertEquals(64, baseline.length());
        assertNotEquals(baseline, RewardPurchaseOperationService.fingerprint("SELF", 4L, 5L, 0L));
        assertNotEquals(baseline, RewardPurchaseOperationService.fingerprint("SELF", 3L, 6L, 0L));
        assertNotEquals(baseline, RewardPurchaseOperationService.fingerprint("MANAGER", 3L, 5L, 0L));
        assertNotEquals(baseline, RewardPurchaseOperationService.fingerprint("SELF", 3L, 5L, 1L));
    }

    @Test
    void unknownReceiptIsNotInterpretedAsReplayPermission() {
        authenticatedSelf();
        UUID key = UUID.randomUUID();
        when(db.query(contains("FROM reward_purchase_operation_receipts"),
                any(RowMapper.class), eq(1L), eq(22L), eq(key))).thenReturn(List.of());
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.receipt(1L, key));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(rewards);
        verify(db, never()).update(anyString(), any(Object[].class));
    }

    @Test void forbiddenForeignWorkspaceNeverReadsOrPurchases() {
        authenticatedSelf();
        when(current.belongsToWorkspace(2L)).thenReturn(false);
        UUID key=UUID.randomUUID();
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
                () -> service.receipt(2L,key)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
                () -> service.purchaseSelf(2L,5L,key)).getStatusCode());
        verifyNoInteractions(epochs,rewards,purchases,db);
    }

    @Test void managerRouteDeniedWithoutRewardPermissionBeforeAnyFinancialWrite() {
        authenticatedSelf();
        var member=mock(com.olehkobylianskyi.familypoints.entity.WorkspaceMember.class);
        var role=mock(com.olehkobylianskyi.familypoints.entity.WorkspaceRole.class);
        when(current.getCurrentMember()).thenReturn(member);
        when(member.getWorkspaceRole()).thenReturn(role);
        UUID key=UUID.randomUUID();
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
                () -> service.purchaseForMember(1L,4L,5L,key)).getStatusCode());
        verifyNoInteractions(epochs,rewards,purchases,db);
    }

    @Test void staleEpochPreventsEvenReceiptLookupWithinNewPurchaseAttempt() {
        authenticatedSelf();
        when(epochs.requireCurrentRequestEpoch(1L)).thenThrow(
                new ResponseStatusException(HttpStatus.CONFLICT,"WORKSPACE_DATA_EPOCH_STALE"));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                () -> service.purchaseSelf(1L,5L,UUID.randomUUID())).getStatusCode());
        verifyNoInteractions(rewards,purchases,db);
    }
}
