package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceRestoreReadOnlyDiagnosticsServiceTest {
    private static final UUID OP=UUID.fromString("11111111-2222-4333-8444-555555555555");
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final WorkspaceRestoreReadOnlyDiagnosticsService service=
            new WorkspaceRestoreReadOnlyDiagnosticsService(db,current);

    private void identity(long family) {
        var account=mock(UserAccount.class);
        var member=mock(WorkspaceMember.class);
        var workspace=mock(Workspace.class);
        when(account.isEnabled()).thenReturn(true);
        when(account.getId()).thenReturn(99L);
        when(member.getId()).thenReturn(123L);
        when(member.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(family);
        when(current.getCurrentAccount()).thenReturn(account);
        when(current.getCurrentMember()).thenReturn(member);
    }
    private void deployed() {
        when(db.queryForObject(anyString(),eq(Boolean.class))).thenReturn(true);
    }
    @Test void refusesWrongSelectedWorkspaceBeforeTouchingDatabase() {
        identity(3);
        assertThrows(SecurityException.class,()->service.assess(4,OP));
        verifyNoInteractions(db);
    }
    @Test void malformedOperationAndMissingIdentityFailClosed() {
        assertThrows(SecurityException.class,()->service.assess(1,null));
        assertThrows(SecurityException.class,()->service.assess(0,OP));
        assertThrows(SecurityException.class,()->service.assess(1,OP));
        verifyNoInteractions(db);
    }
    @Test void missingRestoreSchemaReportsBlockedReadOnlyStatus() {
        identity(7);
        when(db.queryForObject(anyString(),eq(Boolean.class))).thenReturn(false);
        var out=service.assess(7,OP);
        assertEquals(WorkspaceRestoreReadOnlyDiagnosticsService.State.RESTORE_SCHEMA_NOT_DEPLOYED,out.state());
        assertFalse(out.executable());
        verify(db,only()).queryForObject(anyString(),eq(Boolean.class));
    }
    @Test void committedReceiptIsOnlyEvidenceOfRecordedCompletionNotExecutableGrant() {
        identity(7);deployed();
        when(db.queryForList(contains("FROM workspace_restore_receipts"),eq(7L),eq(OP)))
                .thenReturn(List.of(Map.of("target_epoch_before",9L,"target_epoch_after",10L)));
        var out=service.assess(7,OP);
        assertEquals(WorkspaceRestoreReadOnlyDiagnosticsService.State.RECEIPT_COMMITTED,out.state());
        assertEquals(9L,out.epochBefore());
        assertEquals(10L,out.epochAfter());
        assertFalse(out.executable());
        assertEquals(List.of("RESTORE_RECEIPT_READ_ONLY"),out.advisoryCodes());
        verify(db,never()).update(anyString(),any(Object[].class));
        verify(db,never()).update(anyString(),any(Object[].class));
    }
    @Test void inconsistentReceiptDoesNotReportSuccess() {
        identity(7);deployed();
        when(db.queryForList(contains("FROM workspace_restore_receipts"),eq(7L),eq(OP)))
                .thenReturn(List.of(Map.of("target_epoch_before",9L,"target_epoch_after",9L)));
        var out=service.assess(7,OP);
        assertEquals(WorkspaceRestoreReadOnlyDiagnosticsService.State.RECEIPT_INCONSISTENT,out.state());
        assertNull(out.epochAfter());
    }
    @Test void absentOperationReportsOnlyTenantScopedNotFound() {
        identity(7);deployed();
        var out=service.assess(7,OP);
        assertEquals(WorkspaceRestoreReadOnlyDiagnosticsService.State.OPERATION_NOT_FOUND,out.state());
        assertFalse(out.executable());
        verify(db).queryForList(contains("FROM workspace_restore_permits"),eq(7L),eq(OP));
    }
    @Test void consumedPermitWithoutReceiptIsReviewRequiredNeverAutoRetry() {
        identity(7);deployed();
        when(db.queryForList(contains("FROM workspace_restore_permits"),eq(7L),eq(OP)))
                .thenReturn(List.of(Map.of("consumed_at",Timestamp.from(Instant.now()))));
        var out=service.assess(7,OP);
        assertEquals(WorkspaceRestoreReadOnlyDiagnosticsService.State.PERMIT_CONSUMED_WITHOUT_RECEIPT,out.state());
        assertFalse(out.executable());
        assertTrue(out.advisoryCodes().get(0).contains("REVIEW_REQUIRED"));
    }
    @Test void expiredUnusedPermitCannotBeMistakenForSuccess() {
        identity(7);deployed();
        when(db.queryForList(contains("FROM workspace_restore_permits"),eq(7L),eq(OP)))
                .thenReturn(List.of(Map.of("expires_at",Timestamp.from(Instant.now().minusSeconds(60)))));
        var out=service.assess(7,OP);
        assertEquals(WorkspaceRestoreReadOnlyDiagnosticsService.State.PERMIT_EXPIRED_UNUSED,out.state());
        assertFalse(out.executable());
    }
    @Test void futurePermitIsStillNotReceipt() {
        identity(7);deployed();
        when(db.queryForList(contains("FROM workspace_restore_permits"),eq(7L),eq(OP)))
                .thenReturn(List.of(Map.of("expires_at",Timestamp.from(Instant.now().plusSeconds(120)))));
        var out=service.assess(7,OP);
        assertEquals(WorkspaceRestoreReadOnlyDiagnosticsService.State.PERMIT_OUTSTANDING,out.state());
        assertFalse(out.executable());
    }
}
