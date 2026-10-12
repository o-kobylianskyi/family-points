package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest;
import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.RewardService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RewardUpdateAuthorizationTest {
    @Test
    void updateDeniedToMemberWithoutRewardManagementPermission() {
        var current = mock(CurrentUserService.class);
        var member = mock(WorkspaceMember.class);
        var role = mock(WorkspaceRole.class);
        var service = mock(RewardService.class);
        when(current.belongsToWorkspace(8L)).thenReturn(true);
        when(current.getCurrentMember()).thenReturn(member);
        when(member.getWorkspaceRole()).thenReturn(role);

        var controller = new RewardController(service, current);
        assertThrows(IllegalArgumentException.class,
                () -> controller.updateReward(8L, 13L, new RewardDefinitionRequest()));
        verifyNoInteractions(service);
    }

    @Test
    void updateDeniedWhenWorkspaceNotSelected() {
        var current = mock(CurrentUserService.class);
        var service = mock(RewardService.class);
        when(current.belongsToWorkspace(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> new RewardController(service, current).updateReward(
                        99L, 13L, new RewardDefinitionRequest()));
        verifyNoInteractions(service);
    }
}
