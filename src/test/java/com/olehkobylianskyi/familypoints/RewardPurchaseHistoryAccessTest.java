package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardPurchaseHistoryAccessTest {
    @Mock WorkspaceRepository workspaces;
    @Mock WorkspaceMemberRepository members;
    @Mock PointTypeRepository pointTypes;
    @Mock RewardCategoryRepository categories;
    @Mock RewardDefinitionRepository rewards;
    @Mock RewardRequestRepository requests;
    @Mock RewardRequirementRepository requirements;
    @Mock RewardPurchaseRepository purchases;
    @Mock RewardObligationRepository obligations;
    @Mock TaskDefinitionRepository taskDefinitions;
    @Mock TaskInstanceRepository taskInstances;
    @Mock ReputationService reputationService;
    @Mock PointService pointService;
    @Mock RewardAvailabilityService availabilityService;
    @Mock WorkspaceEpochTransactionGuard epochGuard;
    @InjectMocks RewardService service;

    @Test void memberCannotRequestSiblingHistoryOrReachPurchaseQuery() {
        when(members.findByIdAndWorkspaceIdAndActiveTrue(10L,1L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        assertThrows(IllegalArgumentException.class,()->service.purchaseHistory(
                1L,10L,false,11L,25,null,null,null,null));
        verifyNoInteractions(purchases);
    }

    @Test void memberWithoutFilterIsForcedToSelfAndDoesNotWrite() {
        when(members.findByIdAndWorkspaceIdAndActiveTrue(10L,1L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        when(purchases.findHistoryPage(eq(1L),eq(10L),isNull(),isNull(),
                isNull(),isNull(),isNull(),any(Pageable.class)))
                .thenReturn(List.of());
        var result=service.purchaseHistory(1L,10L,false,null,25,null,null,null,null);
        assertTrue(result.items().isEmpty());
        assertFalse(result.hasMore());
        assertNull(result.nextCursor());
        verifyNoInteractions(pointService);
    }

    @Test void rejectsMalformedCursorAndLimitBeforeQuery() {
        when(members.findByIdAndWorkspaceIdAndActiveTrue(10L,1L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        assertThrows(IllegalArgumentException.class,()->service.purchaseHistory(
                1L,10L,false,null,101,null,null,null,null));
        assertThrows(IllegalArgumentException.class,()->service.purchaseHistory(
                1L,10L,false,null,25,"%%%bad",null,null,null));
        assertThrows(IllegalArgumentException.class,()->service.purchaseHistory(
                1L,10L,false,null,25,null,null,
                LocalDateTime.of(2026,11,2,12,0),LocalDateTime.of(2026,11,1,12,0)));
        verifyNoInteractions(purchases);
    }
}
