package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.ResetChallengeAttemptLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceResetChallengeControllerTest {
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private final ResetChallengeAttemptLimiter limiter=mock(ResetChallengeAttemptLimiter.class);
    private final WorkspaceDataResetPreviewController preview=mock(WorkspaceDataResetPreviewController.class);
    private final WorkspaceResetChallengeController controller=
            new WorkspaceResetChallengeController(current,db,encoder,limiter,preview);

    @Test void executeRemainsLockedAndForeignWorkspaceForbidden() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        assertEquals(HttpStatus.LOCKED,
                assertThrows(ResponseStatusException.class,()->controller.execute(7L)).getStatusCode());
        when(current.belongsToWorkspace(7L)).thenReturn(false);
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,()->controller.execute(7L)).getStatusCode());
        verifyNoInteractions(db);
    }

    @Test void invalidChallengeNeverHitsDatabase() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var bad=new WorkspaceResetChallengeController.ChallengeRequest(UUID.randomUUID(),"invalid","password");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class,()->controller.challenge(7L,bad)).getStatusCode());
        verifyNoInteractions(db);
    }

    @Test void challengeRequiresFreshPasswordAndConfirmedOperation() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var account=mock(UserAccount.class);
        when(account.getPasswordHash()).thenReturn("$2a$somehash");
        when(current.getCurrentAccount()).thenReturn(account);
        var request=new WorkspaceResetChallengeController.ChallengeRequest(
                UUID.randomUUID(),"a".repeat(64),"valid-password");
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,()->controller.challenge(7L,request)).getStatusCode());
        verifyNoInteractions(db);
        when(encoder.matches("valid-password","$2a$somehash")).thenReturn(true);
        when(current.getCurrentMemberId()).thenReturn(17L);
        when(db.queryForList(contains("FOR UPDATE"),eq(7L),eq(request.operationId().toString())))
                .thenReturn(List.of(Map.of("requested_by_member_id",17L,
                        "snapshot_digest","a".repeat(64),"status","PREPARED")));
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,()->controller.challenge(7L,request)).getStatusCode());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void confirmedChallengeCreatedOnceButCannotExecute() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var account=mock(UserAccount.class);
        when(account.getPasswordHash()).thenReturn("$2a$somehash");
        when(account.getId()).thenReturn(23L);
        when(current.getCurrentAccount()).thenReturn(account);
        when(current.getCurrentMemberId()).thenReturn(17L);
        when(encoder.matches("pw","$2a$somehash")).thenReturn(true);
        var request=new WorkspaceResetChallengeController.ChallengeRequest(
                UUID.randomUUID(),"b".repeat(64),"pw");
        when(db.queryForList(contains("FOR UPDATE"),eq(7L),eq(request.operationId().toString())))
                .thenReturn(List.of(Map.of("requested_by_member_id",17L,
                        "snapshot_digest","b".repeat(64),"status","CONFIRMED",
                        "selected_categories_json","[]")));
        // Original digest in this test is intentionally arbitrary; challenge must reject stale counts.
        when(preview.preview(7L)).thenReturn(new WorkspaceDataResetPreviewController.ResetPreview(
                7L,false,false,List.of(),Map.of()));
        when(db.update(contains("INSERT INTO workspace_reset_challenges"),any(Object[].class)))
                .thenReturn(1,0);
        assertEquals(HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,()->controller.challenge(7L,request)).getStatusCode());
        verify(db,never()).update(contains("INSERT INTO workspace_reset_challenges"),any(Object[].class));
        assertEquals(HttpStatus.LOCKED,
                assertThrows(ResponseStatusException.class,()->controller.execute(7L)).getStatusCode());
    }
}
