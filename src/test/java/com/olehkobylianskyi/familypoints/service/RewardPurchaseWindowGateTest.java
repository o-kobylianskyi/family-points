package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RewardPurchaseWindowGateTest {
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final RewardDefinitionRepository rewards = mock(RewardDefinitionRepository.class);
    private final RewardRequestRepository requests = mock(RewardRequestRepository.class);
    private final RewardPurchaseRepository purchases = mock(RewardPurchaseRepository.class);
    private final PointService points = mock(PointService.class);
    private final RewardAvailabilityService gate = mock(RewardAvailabilityService.class);

    private RewardService service() {
        return new RewardService(mock(WorkspaceRepository.class),members,
                mock(PointTypeRepository.class),mock(RewardCategoryRepository.class),
                rewards,requests,mock(RewardRequirementRepository.class),purchases,
                mock(RewardObligationRepository.class),mock(TaskDefinitionRepository.class),
                mock(TaskInstanceRepository.class),mock(ReputationService.class),points,gate, org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard.class));
    }

    @Test void directPurchaseOutsideWindowDoesNotChargeOrPersist() {
        var reward = mock(RewardDefinition.class);
        when(members.findByIdAndWorkspaceIdForUpdate(3L,8L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        when(rewards.findForPurchase(11L,8L)).thenReturn(Optional.of(reward));
        when(reward.isActive()).thenReturn(true);
        when(reward.getAcquisitionMode()).thenReturn(RewardAcquisitionMode.DIRECT);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,"REWARD_OUTSIDE_AVAILABILITY_WINDOW"))
                .when(gate).requireOpenForPurchase(reward);

        assertEquals(409,assertThrows(ResponseStatusException.class,
                () -> service().purchaseReward(8L,3L,11L)).getStatusCode().value());
        verify(purchases,never()).save(any());
        verifyNoInteractions(points);
    }

    @Test void approvedCatalogRequestCannotBypassAvailabilityGate() {
        var member = mock(WorkspaceMember.class);
        var reward = mock(RewardDefinition.class);
        var request = mock(RewardRequest.class);
        when(member.getId()).thenReturn(3L);
        when(members.findByIdAndWorkspaceIdForUpdate(3L,8L)).thenReturn(Optional.of(member));
        when(requests.findByIdAndWorkspaceId(15L,8L)).thenReturn(Optional.of(request));
        when(request.getRequestedBy()).thenReturn(member);
        when(request.getStatus()).thenReturn(RewardRequestStatus.READY_TO_PURCHASE);
        when(request.getRewardDefinition()).thenReturn(reward);
        when(reward.getId()).thenReturn(11L);
        when(rewards.findForPurchase(11L,8L)).thenReturn(Optional.of(reward));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,"REWARD_OUTSIDE_AVAILABILITY_WINDOW"))
                .when(gate).requireOpenForPurchase(reward);

        assertEquals(409,assertThrows(ResponseStatusException.class,
                () -> service().purchaseRequest(8L,3L,15L)).getStatusCode().value());
        verify(purchases,never()).save(any());
        verify(request,never()).markPurchased();
        verifyNoInteractions(points);
    }
}
