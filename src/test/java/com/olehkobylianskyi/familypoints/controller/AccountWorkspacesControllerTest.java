package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.*;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.olehkobylianskyi.familypoints.security.WorkspaceAuthenticationDetails;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountWorkspacesControllerTest {
    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void listsOnlySignedInAccountsActiveWorkspaceMemberships() {
        var current = mock(CurrentUserService.class);
        var repository = mock(AccountWorkspaceMembershipRepository.class);
        var familyA = new Workspace("Family A");
        var familyB = new Workspace("Family B");
        var roleA = new WorkspaceRole(familyA, "PARENT", "Parent", true);
        var roleB = new WorkspaceRole(familyB, "PARENT", "Parent", true);
        var memberA = new WorkspaceMember("A", WorkspaceMemberType.PARENT, roleA, familyA);
        var memberB = new WorkspaceMember("B", WorkspaceMemberType.PARENT, roleB, familyB);
        var account = new UserAccount("userA", "hash", memberA);
        setId(familyA, 101L);
        setId(familyB, 202L);
        setId(memberA, 11L);
        setId(memberB, 22L);
        setId(account, 3L);

        when(current.getCurrentAccount()).thenReturn(account);
        var auth = new UsernamePasswordAuthenticationToken("userA", null, List.of());
        auth.setDetails(new WorkspaceAuthenticationDetails(3L, 101L, 11L));
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(repository.findAllByAccountIdOrderByIdAsc(3L)).thenReturn(List.of(
                new AccountWorkspaceMembership(account, memberA),
                new AccountWorkspaceMembership(account, memberB)
        ));

        var response = new AccountWorkspacesController(current, repository).list();
        assertEquals(200, response.getStatusCode().value());
        var choices = response.getBody();
        assertNotNull(choices);
        assertEquals(2, choices.size());
        assertEquals(101L, choices.get(0).workspaceId());
        assertTrue(choices.get(0).active());
        assertEquals(202L, choices.get(1).workspaceId());
        assertFalse(choices.get(1).active());
        verify(repository).findAllByAccountIdOrderByIdAsc(3L);
    }

    @Test
    void returnsEmptyWorkspaceListForFamilylessGlobalAccount() {
        var current = mock(CurrentUserService.class);
        var repository = mock(AccountWorkspaceMembershipRepository.class);
        var account = new UserAccount("new-user", "hash", null);
        setId(account, 42L);
        when(current.getCurrentAccount()).thenReturn(account);
        when(repository.findAllByAccountIdOrderByIdAsc(42L)).thenReturn(List.of());
        var response = new AccountWorkspacesController(current, repository).list();
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
        verify(current, never()).getCurrentWorkspaceId();
    }

    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
