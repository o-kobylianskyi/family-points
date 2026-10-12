package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.ReputationService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyReputationControllerTest {
    @Test
    void usesSelectedWorkspaceMemberInsteadOfLegacyPrimaryMember() {
        var current = mock(CurrentUserService.class);
        var service = mock(ReputationService.class);
        var member = mock(WorkspaceMember.class);
        var workspace = mock(Workspace.class);
        when(current.getCurrentMember()).thenReturn(member);
        when(member.getId()).thenReturn(42L);
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(7L);
        when(service.getBalance(42L)).thenReturn(120L);

        var response = new MyReputationController(current, service, mock(com.olehkobylianskyi.familypoints.repository.ReputationTransactionRepository.class)).balance();

        assertEquals(7L, response.workspaceId());
        assertEquals(42L, response.memberId());
        assertEquals(120L, response.balance());
        verify(service).getBalance(42L);
    }
}
