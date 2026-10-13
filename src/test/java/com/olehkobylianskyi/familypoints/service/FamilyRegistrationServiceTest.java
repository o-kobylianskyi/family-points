package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FamilyRegistrationServiceTest {
    private final FamilyWorkspaceProvisioningService provisioning = mock(FamilyWorkspaceProvisioningService.class);
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final AccountWorkspaceMembershipRepository links = mock(AccountWorkspaceMembershipRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final FamilyRegistrationService registration =
            new FamilyRegistrationService(provisioning, accounts, members, links, encoder);

    @Test
    void refusesWeakPasswordsWithoutWritingAnything() {
        assertThrows(IllegalArgumentException.class,
                () -> registration.register("someone", "short", "Household", "Parent"));
        verifyNoInteractions(provisioning, accounts, members, links, encoder);
    }

    @Test
    void refusesDuplicateUsernameBeforeProvisioningFamily() {
        when(accounts.existsByUsername("someone")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> registration.register("someone", "long-enough-password", "Household", "Parent"));
        verifyNoInteractions(provisioning, members, links, encoder);
    }

    @Test
    void refusesInvalidAccountNames() {
        assertThrows(IllegalArgumentException.class,
                () -> registration.register("bad username", "long-enough-password", "Household", "Parent"));
        verifyNoInteractions(provisioning, accounts, members, links, encoder);
    }
}
