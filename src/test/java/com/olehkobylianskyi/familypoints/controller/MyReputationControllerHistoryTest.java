package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.ReputationTransactionRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.ReputationService;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyReputationControllerHistoryTest {
    @Test
    void readsOnlyAuthenticatedMembersHistory() {
        var current = mock(CurrentUserService.class);
        var history = mock(ReputationTransactionRepository.class);
        var entry = mock(ReputationTransaction.class);
        when(current.getCurrentMemberId()).thenReturn(47L);
        when(entry.getId()).thenReturn(5L);
        when(entry.getAmount()).thenReturn(10);
        when(entry.getSourceType()).thenReturn(ReputationTransactionSourceType.MANUAL);
        when(entry.getDescription()).thenReturn("Helpful action");
        when(entry.getCreatedAt()).thenReturn(LocalDateTime.of(2026,10,12,18,0));
        when(history.findByMemberIdOrderByCreatedAtDesc(47L)).thenReturn(List.of(entry));

        var controller = new MyReputationController(current, mock(ReputationService.class), history);
        var result = controller.history();

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).amount());
        assertEquals("Helpful action", result.get(0).description());
        verify(history).findByMemberIdOrderByCreatedAtDesc(47L);
    }
}
