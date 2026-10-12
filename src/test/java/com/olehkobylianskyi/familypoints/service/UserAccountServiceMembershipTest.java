package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.AccountWorkspaceMembership;
import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAccountServiceMembershipTest {
    @Test
    void createsMembershipForNewAccountInTheSameServiceOperation() {
        var accounts = mock(UserAccountRepository.class);
        var members = mock(WorkspaceMemberRepository.class);
        var memberships = mock(AccountWorkspaceMembershipRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var member = mock(WorkspaceMember.class);

        when(members.findByIdAndWorkspaceId(22L, 11L)).thenReturn(Optional.of(member));
        when(encoder.encode("test-password")).thenReturn("encoded-password");
        when(accounts.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new UserAccountService(accounts, members, encoder, memberships);
        var result = service.createAccount(11L, 22L, "new-account", "test-password");

        assertTrue(result.isPresent());
        assertEquals("new-account", result.orElseThrow().getUsername());
        var link = org.mockito.ArgumentCaptor.forClass(AccountWorkspaceMembership.class);
        verify(memberships).save(link.capture());
        assertSame(result.orElseThrow(), link.getValue().getAccount());
        assertSame(member, link.getValue().getWorkspaceMember());
    }

    @Test
    void rejectsDuplicateAccountWithoutWritingMembership() {
        var accounts = mock(UserAccountRepository.class);
        var members = mock(WorkspaceMemberRepository.class);
        var memberships = mock(AccountWorkspaceMembershipRepository.class);
        var encoder = mock(PasswordEncoder.class);
        when(members.findByIdAndWorkspaceId(22L, 11L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        when(accounts.existsByUsername("duplicate")).thenReturn(true);

        var service = new UserAccountService(accounts, members, encoder, memberships);
        assertThrows(IllegalArgumentException.class,
                () -> service.createAccount(11L, 22L, "duplicate", "test-password"));
        verifyNoInteractions(memberships);
    }
}
