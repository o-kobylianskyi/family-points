package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * P0 evidence: filesystem unlink is not rolled back with SQL. This is NOT an
 * implementation of automatic repair, deletion authorization or Restore.
 */
@Testcontainers
class WorkspaceBackupRetentionRollbackGapPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("retention_rollback_gap")
            .withUsername("retention_gap").withPassword("retention_gap");
    @TempDir Path temp;

    @Test void rollbackAfterPhysicalDeleteLeavesRegistryRowAndReverseAuditReportsMissingBytes()
            throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var transactions=new TransactionTemplate(new DataSourceTransactionManager(ds));
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Retention crash A') RETURNING id",Long.class);
        long neighbor=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Retention crash B') RETURNING id",Long.class);
        long role=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,family);
        long member=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) " +
                "VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,family,role);

        UUID id=UUID.randomUUID();
        var format=new ProtectiveBackupPortableFile();
        var keygen=KeyGenerator.getInstance("AES");
        keygen.init(256);
        var encrypted=format.encode(new ProtectiveBackupArtifactCodec().seal(
                id,family,UUID.randomUUID(),0,"a".repeat(64),"V50",
                "{\"format\":\"test\",\"categories\":{}}".getBytes(StandardCharsets.UTF_8),
                keygen.generateKey()));
        var store=new WorkspaceBackupLocalArtifactStore(temp.resolve("backups").toString(),format);
        var stored=store.store(family,id,encrypted);
        Path artifact=temp.resolve("backups").resolve(Long.toString(family))
                .resolve(id+".fpbackup");
        assertTrue(Files.exists(artifact));
        db.update("""
                INSERT INTO workspace_protective_backups
                    (snapshot_id,workspace_id,operation_id,requested_by_member_id,
                     data_epoch,reset_mode,selected_categories_json,source_digest,
                     schema_version,state,storage_key,encrypted,artifact_sha256,
                     artifact_bytes,artifact_stored_at,created_at,expires_at)
                VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,
                        'V50','BACKING_UP',?,TRUE,?,?,NOW()-INTERVAL '35 days',
                        NOW()-INTERVAL '40 days',NOW()-INTERVAL '1 day')
                """,id,family,UUID.randomUUID(),member,"a".repeat(64),
                "local:v1:"+family+"/"+id+".fpbackup",stored.sha256(),stored.byteCount());

        var retention=new WorkspaceBackupRetentionService(db,store);
        var failure=assertThrows(IllegalStateException.class,()->transactions.execute(status->{
            assertEquals(1,retention.expireEligible(1));
            assertFalse(Files.exists(artifact),"Physical delete already occurred");
            throw new IllegalStateException("SIMULATED_SQL_ROLLBACK_AFTER_DELETE");
        }));
        assertEquals("SIMULATED_SQL_ROLLBACK_AFTER_DELETE",failure.getMessage());

        assertEquals("BACKING_UP",db.queryForObject(
                "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                String.class,id));
        assertFalse(Files.exists(artifact),
                "SQL rollback cannot re-create ciphertext already unlinked");
        var evidence=new WorkspaceBackupRegistryPresenceAuditService(db,store)
                .audit(family,10);
        assertEquals(1,evidence.size());
        assertEquals("REGISTRY_CIPHERTEXT_INVALID",evidence.get(0).classification());
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",
                Long.class,neighbor));
        assertFalse(Files.exists(artifact),
                "Read-only recovery audit never repairs, deletes, or fabricates archived bytes");
        // Both tests share this class's container; remove synthetic metadata
        // so the other retention test cannot select a stale earlier row.
        assertEquals(1,db.update(
                "DELETE FROM workspace_protective_backups WHERE snapshot_id=?",id));
    }

    @Test void unlinkFailureRollsBackSqlInsteadOfMarkingRegistryExpired() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Retention unlink failure') RETURNING id",
                Long.class);
        long role=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,family);
        long member=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) " +
                "VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,family,role);
        UUID id=UUID.randomUUID();
        db.update("""
                INSERT INTO workspace_protective_backups
                    (snapshot_id,workspace_id,operation_id,requested_by_member_id,
                     data_epoch,reset_mode,selected_categories_json,source_digest,
                     schema_version,state,storage_key,encrypted,artifact_sha256,
                     artifact_bytes,artifact_stored_at,created_at,expires_at)
                VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,
                        'V50','BACKING_UP',?,TRUE,?,120,NOW()-INTERVAL '35 days',
                        NOW()-INTERVAL '40 days',NOW()-INTERVAL '1 day')
                """,id,family,UUID.randomUUID(),member,"a".repeat(64),
                "local:v1:"+family+"/"+id+".fpbackup","b".repeat(64));
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        doThrow(new IllegalStateException("DISK_UNLINK_DENIED"))
                .when(storage).deleteAuthorizedExpired(family,id);
        var retention=new WorkspaceBackupRetentionService(db,storage);
        assertEquals("DISK_UNLINK_DENIED",assertThrows(IllegalStateException.class,
                ()->tx.execute(status->retention.expireEligible(1))).getMessage());
        assertEquals("BACKING_UP",db.queryForObject(
                "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                String.class,id));
        verify(storage).deleteAuthorizedExpired(family,id);
        assertEquals(1,db.update(
                "DELETE FROM workspace_protective_backups WHERE snapshot_id=?",id));
    }
}
