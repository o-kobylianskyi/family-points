package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovedRewardPurchaseTest {
    @Test
    void approvedRequestDebitsOnceAndRejectsRepeatedPurchase() {
        var members=mock(WorkspaceMemberRepository.class);
        var requests=mock(RewardRequestRepository.class);
        var purchases=mock(RewardPurchaseRepository.class);
        var points=mock(PointService.class);
        var obligations=mock(RewardObligationRepository.class);
        var requirements=mock(RewardRequirementRepository.class);
        var workspaces=mock(WorkspaceRepository.class);
        var member=mock(WorkspaceMember.class);
        var workspace=mock(Workspace.class);
        var pointType=mock(PointType.class);
        when(member.getId()).thenReturn(3L);
        when(workspace.getId()).thenReturn(8L);
        when(pointType.getId()).thenReturn(5L);
        when(members.findByIdAndWorkspaceIdForUpdate(3L,8L)).thenReturn(Optional.of(member));
        when(workspaces.findById(8L)).thenReturn(Optional.of(workspace));
        when(obligations.findByMemberIdAndStatusOrderByCreatedAtAsc(3L,RewardObligationStatus.OPEN))
                .thenReturn(List.of());
        when(requirements.findByRewardRequestIdAndPhaseOrderBySortOrderAscIdAsc(
                77L,RewardRequirementPhase.AFTER_REWARD)).thenReturn(List.of());
        var request=new RewardRequest(workspace,member,null,"20-point reward",null);
        request.approve(pointType,20,null,null,false);
        when(requests.findByIdAndWorkspaceId(77L,8L)).thenReturn(Optional.of(request));
        when(purchases.save(any(RewardPurchase.class))).thenAnswer(inv->inv.getArgument(0));

        var service=new RewardService(workspaces,members,mock(PointTypeRepository.class),
                mock(RewardCategoryRepository.class),mock(RewardDefinitionRepository.class),
                requests,requirements,purchases,obligations,mock(TaskDefinitionRepository.class),
                mock(TaskInstanceRepository.class),mock(ReputationService.class),points);

        service.purchaseRequest(8L,3L,77L);
        assertEquals(RewardRequestStatus.PURCHASED,request.getStatus());
        verify(points).spend(eq(8L),eq(3L),eq(5L),eq(20),
                eq(PointTransactionSourceType.REWARD_PURCHASE),any(),anyString());
        assertThrows(IllegalArgumentException.class,()->service.purchaseRequest(8L,3L,77L));
        verify(purchases,times(1)).save(any(RewardPurchase.class));
        verify(points,times(1)).spend(any(),any(),any(),anyInt(),any(),any(),anyString());
    }
}
