package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FirstFamilyOnboardingServiceTest {
    final CurrentUserService current = mock(CurrentUserService.class);
    final UserAccountRepository accounts = mock(UserAccountRepository.class);
    final AccountWorkspaceMembershipRepository memberships = mock(AccountWorkspaceMembershipRepository.class);
    final AdditionalFamilyService families = mock(AdditionalFamilyService.class);
    final FirstFamilyOnboardingService service =
            new FirstFamilyOnboardingService(current, accounts, memberships, families);

    @Test
    void createsFirstFamilyForAccountWithoutMembership() {
        var account = mock(UserAccount.class);
        when(account.getId()).thenReturn(1L);
        when(account.isEnabled()).thenReturn(true);
        when(current.getCurrentAccount()).thenReturn(account);
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(memberships.findAllByAccountIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(families.create("Family", "Owner")).thenReturn(new AdditionalFamilyService.Created(9L, 4L));
        var result = service.create("Family", "Owner");
        assertEquals(9L, result.workspaceId());
        verify(families).create("Family", "Owner");
    }

    @Test
    void refusesSecondFirstFamily() {
        var account = mock(UserAccount.class);
        when(account.getId()).thenReturn(1L);
        when(account.isEnabled()).thenReturn(true);
        when(current.getCurrentAccount()).thenReturn(account);
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(account.getWorkspaceMember()).thenReturn(mock(com.olehkobylianskyi.familypoints.entity.WorkspaceMember.class));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.create("Again", "Owner")).getStatusCode().value());
        verifyNoInteractions(families);
    }
}
