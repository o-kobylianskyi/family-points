package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RewardPurchaseCooldownTest {
    @Test
    void recentPurchaseBlocksNewPurchaseBeforePointsAreDebited() {
        var members = mock(WorkspaceMemberRepository.class);
        var rewards = mock(RewardDefinitionRepository.class);
        var purchases = mock(RewardPurchaseRepository.class);
        var points = mock(PointService.class);
        var member = mock(WorkspaceMember.class);
        var reward = mock(RewardDefinition.class);
        var previous = mock(RewardPurchase.class);
        when(members.findByIdAndWorkspaceIdForUpdate(3L, 8L)).thenReturn(Optional.of(member));
        when(rewards.findByIdAndWorkspaceId(11L, 8L)).thenReturn(Optional.of(reward));
        when(reward.isActive()).thenReturn(true);
        when(reward.getAcquisitionMode()).thenReturn(RewardAcquisitionMode.DIRECT);
        when(reward.getCooldownSeconds()).thenReturn(60);
        when(reward.getId()).thenReturn(11L);
        when(purchases.findFirstByMemberIdAndRewardDefinitionIdOrderByPurchasedAtDesc(3L, 11L))
                .thenReturn(Optional.of(previous));
        when(previous.getPurchasedAt()).thenReturn(LocalDateTime.now().minusSeconds(5));
        when(previous.getStatus()).thenReturn(RewardPurchaseStatus.READY);

        var service = new RewardService(mock(WorkspaceRepository.class), members,
                mock(PointTypeRepository.class), mock(RewardCategoryRepository.class),
                rewards, mock(RewardRequestRepository.class),
                mock(RewardRequirementRepository.class), purchases,
                mock(RewardObligationRepository.class), mock(TaskDefinitionRepository.class),
                mock(TaskInstanceRepository.class), mock(ReputationService.class), points);

        var failure = assertThrows(ResponseStatusException.class,
                () -> service.purchaseReward(8L, 3L, 11L));
        assertEquals(409, failure.getStatusCode().value());
        verifyNoInteractions(points);
        verify(purchases, never()).save(any());
    }
}
