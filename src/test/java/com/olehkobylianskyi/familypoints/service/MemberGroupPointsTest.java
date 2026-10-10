package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.exception.InsufficientPointsException;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberGroupPointsTest {
    @Mock WorkspaceRepository workspaces;
    @Mock WorkspaceMemberRepository members;
    @Mock MemberGroupRepository groups;
    @Mock GroupMembershipRepository memberships;
    @Mock GroupCompositionRepository compositions;
    @Mock PointTypeRepository pointTypes;
    @Mock GroupPointTransactionRepository groupLedger;
    @Mock RoleSetRepository roleSets;
    @Mock RoleDefinitionRepository roleDefinitions;
    @Mock RoleAssignmentRepository roleAssignments;
    @Mock RolePermissionGrantRepository rolePermissionGrants;
    @InjectMocks MemberGroupService service;

    @Test
    void spendAboveBalanceIsRejectedWithoutLedgerWrite() {
        MemberGroup group = mock(MemberGroup.class);
        PointType type = mock(PointType.class);
        when(group.isActive()).thenReturn(true);
        when(groups.findWithLockByIdAndWorkspaceId(3L, 1L)).thenReturn(Optional.of(group));
        when(pointTypes.findByIdAndWorkspaceId(5L, 1L)).thenReturn(Optional.of(type));
        when(groupLedger.getBalance(3L, 5L)).thenReturn(10L);

        InsufficientPointsException error = assertThrows(
                InsufficientPointsException.class,
                () -> service.addPoints(1L, 3L, 5L, 11, PointTransactionType.SPEND, "Test")
        );

        assertEquals(11L, error.getRequired());
        assertEquals(10L, error.getAvailable());
        verify(groupLedger, never()).save(any(GroupPointTransaction.class));
    }

    @Test
    void zeroAndNegativeManualAmountsAreRejected() {
        MemberGroup group = mock(MemberGroup.class);
        PointType type = mock(PointType.class);
        when(group.isActive()).thenReturn(true);
        when(groups.findWithLockByIdAndWorkspaceId(3L, 1L)).thenReturn(Optional.of(group));
        when(pointTypes.findByIdAndWorkspaceId(5L, 1L)).thenReturn(Optional.of(type));

        assertThrows(IllegalArgumentException.class,
                () -> service.addPoints(1L, 3L, 5L, 0, PointTransactionType.EARN, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.addPoints(1L, 3L, 5L, -1, PointTransactionType.SPEND, null));
        verify(groupLedger, never()).save(any(GroupPointTransaction.class));
    }
}
