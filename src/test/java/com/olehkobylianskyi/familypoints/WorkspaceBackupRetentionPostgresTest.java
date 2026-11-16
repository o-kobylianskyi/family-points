package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.time.Duration;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class WorkspaceBackupRetentionPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("backup_retention").withUsername("retention")
            .withPassword("retention_testing_only");

    @Test void expiredFileIsLockedByActiveRestoreLeaseAndCannotCrossTenantBoundary() {
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(source);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        var service=new WorkspaceBackupRetentionService(db,store);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Retention A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Retention B') RETURNING id",Long.class);
        long role=db.queryForObject("INSERT INTO family_roles(family_id,system_role,code,name) VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,a);
        long member=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,active,member_type,name) VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,a,role);
        UUID snapshot=UUID.randomUUID(),lease=UUID.randomUUID();
        db.update("""
            INSERT INTO workspace_protective_backups
              (snapshot_id,workspace_id,operation_id,requested_by_member_id,
               data_epoch,reset_mode,selected_categories_json,source_digest,schema_version,
               state,storage_key,encrypted,artifact_sha256,artifact_bytes,artifact_stored_at,
               created_at,expires_at)
            VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,'V51',
                'BACKING_UP',?,TRUE,?,120,NOW()-INTERVAL '30 days',
                NOW()-INTERVAL '32 days',NOW()-INTERVAL '1 day')
            """,snapshot,a,UUID.randomUUID(),member,"a".repeat(64),
            "local:v1:"+a+"/"+snapshot+".fpbackup","b".repeat(64));
        db.update("""
            INSERT INTO workspace_backup_restore_leases
              (lease_id,snapshot_id,workspace_id,expires_at)
            VALUES (?,?,?,NOW()+INTERVAL '5 minutes')
            """,lease,snapshot,a);
        assertEquals(0,tx.execute(s->service.expireEligible(10)).intValue());
        verifyNoInteractions(store);
        assertFalse(tx.execute(s->service.release(b,snapshot,lease)).booleanValue());
        assertTrue(tx.execute(s->service.release(a,snapshot,lease)).booleanValue());
        assertEquals(1,tx.execute(s->service.expireEligible(10)).intValue());
        verify(store).deleteAuthorizedExpired(a,snapshot);
        assertEquals("EXPIRED",db.queryForObject("SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",String.class,snapshot));
        assertEquals(0,tx.execute(s->service.expireEligible(10)).intValue());
    }

    @Test void unverifiedBackupCannotAcquireRestoreLease() {
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(source);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Unverified') RETURNING id",Long.class);
        long role=db.queryForObject("INSERT INTO family_roles(family_id,system_role,code,name) VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,a);
        long member=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,active,member_type,name) VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,a,role);
        UUID snapshot=UUID.randomUUID();
        db.update("""
            INSERT INTO workspace_protective_backups
              (snapshot_id,workspace_id,operation_id,requested_by_member_id,
               data_epoch,reset_mode,selected_categories_json,source_digest,schema_version)
            VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,'V51')
            """,snapshot,a,UUID.randomUUID(),member,"a".repeat(64));
        var service=new WorkspaceBackupRetentionService(db,mock(WorkspaceBackupLocalArtifactStore.class));
        assertThrows(ResponseStatusException.class,()->tx.execute(s->
                service.acquireVerifiedLease(a,snapshot,Duration.ofMinutes(5))));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM workspace_backup_restore_leases WHERE snapshot_id=?",Long.class,snapshot));
        // Simulate a separate, independently verified rehearsal result inside
        // disposable PostgreSQL, not a production verification code path.
        db.update("""
            UPDATE workspace_protective_backups
            SET state='VERIFIED',encrypted=TRUE,storage_key=?,
                manifest_sha256=?,restore_verified_at=NOW(),verified_at=NOW(),
                artifact_sha256=?,artifact_bytes=120,artifact_stored_at=NOW()
            WHERE snapshot_id=? AND workspace_id=?
            ""","local:v1:"+a+"/"+snapshot+".fpbackup",
                "c".repeat(64),"b".repeat(64),snapshot,a);
        UUID lease=tx.execute(s->service.acquireVerifiedLease(a,snapshot,Duration.ofMinutes(5)));
        assertNotNull(lease);
        assertEquals(1L,db.queryForObject("""
                SELECT count(*) FROM workspace_backup_restore_leases
                WHERE snapshot_id=? AND workspace_id=? AND expires_at>NOW()
                """,Long.class,snapshot,a));
        assertTrue(tx.execute(s->service.release(a,snapshot,lease)).booleanValue());
        assertFalse(tx.execute(s->service.release(a,snapshot,lease)).booleanValue());
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->
                service.acquireVerifiedLease(a,snapshot,Duration.ofMinutes(16))));
    }
}
