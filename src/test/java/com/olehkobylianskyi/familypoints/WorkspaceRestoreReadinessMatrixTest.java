package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceResetDependencyAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** BKL002 management-facing inventory is informative, never executable. */
class WorkspaceRestoreReadinessMatrixTest {
    @Test void allExportableCategoriesRemainBlockedDespiteExistingForeignKeys() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        var service=spy(new WorkspaceResetDependencyAuditService(jdbc));
        var fk=new WorkspaceResetDependencyAuditService.ForeignKey(
                "task_instances","task_definition_id","task_definitions","id",
                "fk_task_instance_definition","CASCADE");
        var protectedFk=new WorkspaceResetDependencyAuditService.ForeignKey(
                "point_transactions","member_id","family_members","id",
                "fk_transaction_member","NO_ACTION");
        doReturn(new WorkspaceResetDependencyAuditService.Audit(
                7L,false,false,List.of("families","family_members"),
                List.of(fk,protectedFk),List.of("RESET_EXECUTION_DISABLED")))
                .when(service).audit(7L);
        var result=service.restoreReadiness(7L);
        assertEquals(7L,result.workspaceId());
        assertFalse(result.fullSupported());
        assertFalse(result.selectiveExecuteEnabled());
        assertEquals(13,result.categories().size());
        assertTrue(result.categories().stream().noneMatch(
                WorkspaceResetDependencyAuditService.CategoryReadiness::supportedForExecute));
        var tasks=result.categories().stream()
                .filter(row->row.category().equals("TASK_DEFINITIONS")).findFirst().orElseThrow();
        assertEquals(1,tasks.incomingForeignKeys().size());
        assertTrue(tasks.blockers().contains("INBOUND_FK_DEPENDENCY_CLOSURE_UNVERIFIED"));
        var ledger=result.categories().stream()
                .filter(row->row.category().equals("POINT_LEDGER")).findFirst().orElseThrow();
        assertTrue(ledger.blockers().contains("FINANCIAL_INTEGRITY_AND_HISTORY_POLICY_REQUIRED"));
        assertTrue(result.globalBlockers().contains("FULL_RESTORE_UNSUPPORTED"));
        assertTrue(result.globalBlockers().contains("RESET_EXECUTION_DISABLED"));
        verifyNoInteractions(jdbc);
    }
}
