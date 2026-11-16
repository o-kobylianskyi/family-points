package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.crypto.KeyGenerator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Early abort occurs before serializing any encrypted bytes; no ledger or
 * family rows are ever mutated. Exact SQL semantics receive PostgreSQL QA.
 */
class WorkspacePortableArchiveForeignRelationTest {
    private void assertRejected(String category,String SQL,String expected) throws Exception {
        var db=mock(JdbcTemplate.class);
        var snapshots=mock(WorkspaceResetSnapshotService.class);
        when(snapshots.digest(eq(17L),any())).thenReturn("a".repeat(64));
        when(db.queryForObject(eq("SELECT data_epoch FROM families WHERE id=?"),
                eq(Long.class),eq(17L))).thenReturn(0L);
        when(db.queryForObject(contains(SQL),eq(Long.class),eq(17L))).thenReturn(1L);
        var exporter=new WorkspacePortableBackupExporter(db,snapshots,
                new ProtectiveBackupArtifactCodec(),new ProtectiveBackupPortableFile());
        var generator=KeyGenerator.getInstance("AES");
        generator.init(256);
        var key=generator.generateKey();
        var failure=assertThrows(SecurityException.class,()->exporter.export(
                17L,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of(category),"V50",key));
        assertEquals(expected,failure.getMessage());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void taskExecutionCannotBorrowDifferentFamilyTaskDefinition() throws Exception {
        assertRejected("TASK_EXECUTIONS","FROM task_instances t",
                "BACKUP_TASK_EXECUTION_FOREIGN_DEFINITION");
    }

    @Test void rewardPurchaseCannotBorrowForeignRewardRequestOrCurrency() throws Exception {
        assertRejected("REWARD_PURCHASES","FROM reward_purchases t",
                "BACKUP_REWARD_PURCHASE_FOREIGN_RELATION");
    }

    @Test void rewardRequestCannotBorrowForeignApproverIdentityOrReward() throws Exception {
        assertRejected("REWARD_REQUESTS","FROM reward_requests t",
                "BACKUP_REWARD_REQUEST_FOREIGN_RELATION");
    }
}
