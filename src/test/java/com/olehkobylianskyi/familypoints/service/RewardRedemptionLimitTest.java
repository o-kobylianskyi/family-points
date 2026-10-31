package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RewardRedemptionLimitTest {
    @Test
    void memberTotalLimitBlocksRepeatWithoutDebitingPoints() {
        var members = mock(WorkspaceMemberRepository.class);
        var rewards = mock(RewardDefinitionRepository.class);
        var purchases = mock(RewardPurchaseRepository.class);
        var points = mock(PointService.class);
        var reward = mock(RewardDefinition.class);
        when(members.findByIdAndWorkspaceIdForUpdate(3L, 8L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        when(rewards.findForPurchase(11L, 8L)).thenReturn(Optional.of(reward));
        when(reward.isActive()).thenReturn(true);
        when(reward.getAcquisitionMode()).thenReturn(RewardAcquisitionMode.DIRECT);
        when(reward.getId()).thenReturn(11L);
        when(reward.getMemberTotalLimit()).thenReturn(1);
        when(purchases.countForLimit(eq(11L), eq(3L), isNull(), isNull())).thenReturn(1L);

        var service = new RewardService(mock(WorkspaceRepository.class), members,
                mock(PointTypeRepository.class), mock(RewardCategoryRepository.class), rewards,
                mock(RewardRequestRepository.class), mock(RewardRequirementRepository.class),
                purchases, mock(RewardObligationRepository.class),
                mock(TaskDefinitionRepository.class), mock(TaskInstanceRepository.class),
                mock(ReputationService.class), points, mock(RewardAvailabilityService.class), org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard.class));

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.purchaseReward(8L, 3L, 11L));
        assertEquals(409, failure.getStatusCode().value());
        verifyNoInteractions(points);
        verify(purchases, never()).save(any());
    }

    @Test
    void workspaceLimitChecksAllMembersNotJustBuyer() {
        var members = mock(WorkspaceMemberRepository.class);
        var rewards = mock(RewardDefinitionRepository.class);
        var purchases = mock(RewardPurchaseRepository.class);
        var points = mock(PointService.class);
        var reward = mock(RewardDefinition.class);
        when(members.findByIdAndWorkspaceIdForUpdate(3L, 8L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        when(rewards.findForPurchase(11L, 8L)).thenReturn(Optional.of(reward));
        when(reward.isActive()).thenReturn(true);
        when(reward.getAcquisitionMode()).thenReturn(RewardAcquisitionMode.DIRECT);
        when(reward.getId()).thenReturn(11L);
        // Mockito returns zero for unstubbed boxed Integer getters; production uses null for unlimited.
        when(reward.getMemberTotalLimit()).thenReturn((Integer) null);
        when(reward.getMemberDailyLimit()).thenReturn((Integer) null);
        when(reward.getMemberWeeklyLimit()).thenReturn((Integer) null);
        when(reward.getWorkspaceTotalLimit()).thenReturn(2);
        when(purchases.countForLimit(eq(11L), isNull(), isNull(), isNull())).thenReturn(2L);

        var service = new RewardService(mock(WorkspaceRepository.class), members,
                mock(PointTypeRepository.class), mock(RewardCategoryRepository.class), rewards,
                mock(RewardRequestRepository.class), mock(RewardRequirementRepository.class),
                purchases, mock(RewardObligationRepository.class),
                mock(TaskDefinitionRepository.class), mock(TaskInstanceRepository.class),
                mock(ReputationService.class), points, mock(RewardAvailabilityService.class), org.mockito.Mockito.mock(com.olehkobylianskyi.familypoints.service.WorkspaceEpochTransactionGuard.class));

        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.purchaseReward(8L, 3L, 11L)).getStatusCode().value());
        verify(purchases).countForLimit(eq(11L), isNull(), isNull(), isNull());
        verifyNoInteractions(points);
    }
}
