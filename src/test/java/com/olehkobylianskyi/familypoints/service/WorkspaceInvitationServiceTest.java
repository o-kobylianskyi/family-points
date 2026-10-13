package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Fast security regression checks. DB concurrency and Flyway are tested separately.
 */
class WorkspaceInvitationServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CurrentUserService user = mock(CurrentUserService.class);
    private final WorkspaceInvitationService invitations = new WorkspaceInvitationService(jdbc, user);

    @Test
    void rejectsInvalidContextHandleBeforeQueryingDatabase() {
        assertThrows(ResponseStatusException.class, () -> invitations.resume("not-a-valid-handle"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsInvalidInvitationTokenBeforeQueryingDatabase() {
        assertThrows(ResponseStatusException.class, () -> invitations.preview("short"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsAdministrativeRoleDuringIssuance() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(15L), eq(8L))).thenReturn(0);
        assertThrows(ResponseStatusException.class, () -> invitations.issue(8L, 15L));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsAlreadyConsumedOrExpiredContext() {
        String validShape = "A".repeat(43);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), anyString()))
                .thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> invitations.resume(validShape));
    }

    @Test
    void cannotAcceptInvalidMemberName() {
        String validShape = "A".repeat(43);
        assertThrows(ResponseStatusException.class, () -> invitations.accept(validShape, " "));
        verifyNoInteractions(jdbc);
    }

    @Test
    void cannotAcceptMalformedHandle() {
        assertThrows(ResponseStatusException.class, () -> invitations.accept("broken", "Parent"));
        verifyNoInteractions(jdbc);
    }
}
