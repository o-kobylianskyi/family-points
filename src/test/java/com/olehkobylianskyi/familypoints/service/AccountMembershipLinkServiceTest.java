package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountMembershipLinkServiceTest {
    private final AccountWorkspaceMembershipRepository links = mock(AccountWorkspaceMembershipRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final AccountMembershipLinkService service = new AccountMembershipLinkService(links, members);

    private UserAccount account() {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(5L);
        when(account.isEnabled()).thenReturn(true);
        return account;
    }

    @Test
    void linksAnActiveUnclaimedMember() {
        var account = account();
        var member = mock(WorkspaceMember.class);
        when(members.findByIdAndWorkspaceIdAndActiveTrue(7L, 9L)).thenReturn(Optional.of(member));
        when(links.save(any(AccountWorkspaceMembership.class))).thenAnswer(i -> i.getArgument(0));
        var link = service.linkExistingAccount(account, 9L, 7L);
        assertSame(account, link.getAccount());
        assertSame(member, link.getWorkspaceMember());
        verify(links).save(any(AccountWorkspaceMembership.class));
    }

    @Test
    void deniesAlreadyClaimedMember() {
        when(members.findByIdAndWorkspaceIdAndActiveTrue(7L, 9L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        when(links.existsByWorkspaceMemberId(7L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.linkExistingAccount(account(), 9L, 7L));
        verify(links, never()).save(any());
    }

    @Test
    void deniesSecondLinkToSameWorkspace() {
        when(members.findByIdAndWorkspaceIdAndActiveTrue(7L, 9L))
                .thenReturn(Optional.of(mock(WorkspaceMember.class)));
        when(links.existsByAccountIdAndWorkspaceMemberWorkspaceId(5L, 9L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.linkExistingAccount(account(), 9L, 7L));
        verify(links, never()).save(any());
    }

    @Test
    void deniesMissingOrInactiveMember() {
        assertThrows(IllegalArgumentException.class, () -> service.linkExistingAccount(account(), 9L, 7L));
        verify(links, never()).save(any());
    }
}
