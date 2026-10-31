package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PointMutationServiceTest {
    private final WorkspaceEpochTransactionGuard guard = mock(WorkspaceEpochTransactionGuard.class);
    private final PointService points = mock(PointService.class);
    private final PointMutationService service = new PointMutationService(guard, points);

    @Test void staleEpochRejectsEveryEntryBeforeLedgerOrReplayLookup() {
        when(guard.requireCurrentEpoch(7L, 2L))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));
        assertThrows(ResponseStatusException.class, () -> service.applyOfflineOperation(
                7L, 9L, "earn", new OfflinePointOperationRequest(), 2L));
        assertThrows(ResponseStatusException.class, () -> service.applyTypedManualOperation(
                7L, 9L, "earn", new PointOperationTypedRequest(), 2L));
        for (String operation : new String[]{"earn", "spend", "penalty"}) {
            assertThrows(ResponseStatusException.class, () -> service.applyManualOperation(
                    7L, 9L, operation, new PointOperationRequest(), 2L));
        }
        verifyNoInteractions(points);
    }

    @Test void offlineKeyCurrencyAndOriginalEpochReachGuardBeforePosting() {
        var request = new OfflinePointOperationRequest();
        request.setOperationId(UUID.randomUUID());
        request.setPointTypeId(11L);
        request.setAmount(5);
        request.setDescription("fixture");
        service.applyOfflineOperation(7L, 9L, "earn", request, 3L);
        var order = inOrder(guard, points);
        order.verify(guard).requireCurrentEpoch(7L, 3L);
        order.verify(points).applyOfflineOperation(7L, 9L, request.getOperationId(),
                11L, "earn", 5, "fixture");
    }

    @Test void legacyMissingEpochIsForwardedWithoutInventingCurrentGeneration() {
        var request = new PointOperationRequest();
        request.setAmount(5);
        service.applyManualOperation(7L, 9L, "penalty", request, null);
        var order = inOrder(guard, points);
        order.verify(guard).requireCurrentEpoch(7L, null);
        order.verify(points).penalty(7L, 9L, 5, null);
    }
}
