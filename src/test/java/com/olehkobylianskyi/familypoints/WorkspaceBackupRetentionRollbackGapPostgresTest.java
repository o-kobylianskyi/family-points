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
 * RET-01 regression: SQL rollback must NEVER unlink the sole ciphertext.
 * Physical purge remains disabled; these tests use disposable PostgreSQL.
 */
@Testcontainers
class WorkspaceBackupRetentionRollbackGapPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("retention_rollback_gap")
            .withUsername("retention_gap").withPassword("retention_gap");
    @TempDir Path temp;

    @Test void rollbackAfterExpiryTransitionPreservesCiphertextAndRegistry()
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
            assertTrue(Files.exists(artifact),"SQL expiry may not unlink ciphertext");
            throw new IllegalStateException("SIMULATED_SQL_ROLLBACK_AFTER_EXPIRY");
        }));
        assertEquals("SIMULATED_SQL_ROLLBACK_AFTER_EXPIRY",failure.getMessage());

        assertEquals("BACKING_UP",db.queryForObject(
                "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                String.class,id));
        assertTrue(Files.exists(artifact),
                "Rollback must leave original ciphertext recoverable");
        var evidence=new WorkspaceBackupRegistryPresenceAuditService(db,store)
                .audit(family,10);
        assertEquals(1,evidence.size());
        assertNotEquals("REGISTRY_CIPHERTEXT_INVALID",evidence.get(0).classification());
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",
                Long.class,neighbor));
        assertTrue(Files.exists(artifact),
                "Read-only audit cannot destroy archived bytes");
        // Both tests share this class's container; remove synthetic metadata
        // so the other retention test cannot select a stale earlier row.
        assertEquals(1,db.update(
                "DELETE FROM workspace_protective_backups WHERE snapshot_id=?",id));
    }

    @Test void committedExpiryRetainsBytesAndRepeatedSweepCannotReissueLease()
            throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Retention committed safe') RETURNING id",Long.class);
        long role=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) VALUES (?,true,'PARENT','Parent') RETURNING id",
                Long.class,family);
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
        var store=new WorkspaceBackupLocalArtifactStore(temp.resolve("committed").toString(),format);
        var stored=store.store(family,id,encrypted);
        Path artifact=temp.resolve("committed").resolve(Long.toString(family))
                .resolve(id+".fpbackup");
        db.update("""
                INSERT INTO workspace_protective_backups
                    (snapshot_id,workspace_id,operation_id,requested_by_member_id,
                     data_epoch,reset_mode,selected_categories_json,source_digest,
                     schema_version,state,storage_key,encrypted,artifact_sha256,
                     artifact_bytes,artifact_stored_at,created_at,expires_at,
                     restore_verified_at,verified_at,manifest_sha256)
                VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,
                        'V50','VERIFIED',?,TRUE,?,?,NOW()-INTERVAL '35 days',
                        NOW()-INTERVAL '40 days',NOW()-INTERVAL '1 day',
                        NOW()-INTERVAL '2 days',NOW()-INTERVAL '2 days',?)
                """,id,family,UUID.randomUUID(),member,"a".repeat(64),
                "local:v1:"+family+"/"+id+".fpbackup",stored.sha256(),stored.byteCount(),
                "c".repeat(64));
        var retention=new WorkspaceBackupRetentionService(db,store);
        assertEquals(1, (int)tx.execute(status->retention.expireEligible(10)));
        assertEquals("EXPIRED",db.queryForObject(
                "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                String.class,id));
        assertTrue(Files.isRegularFile(artifact),"Committed expiry never purges ciphertext");
        // A process restart has no in-memory cleanup cursor to lose: the
        // committed EXPIRED row remains inspectable and no sweep can unlink.
        var restartedRetention=new WorkspaceBackupRetentionService(db,store);
        assertEquals(0, (int)tx.execute(status->restartedRetention.expireEligible(10)));
        assertEquals("RETAINED_VALID",
                restartedRetention.inspectExpiredArtifactsForWorkspace(family,10)
                        .stream().filter(row->id.equals(row.snapshotId()))
                        .findFirst().orElseThrow().status());
        assertTrue(Files.isRegularFile(artifact));
        assertEquals(0, (int)tx.execute(status->retention.expireEligible(10)));
        assertEquals(1,retention.expiredArtifactInventory(10).stream()
                .filter(row->id.equals(row.snapshotId())).count());
        assertEquals("RETAINED_VALID",retention.inspectExpiredArtifacts(10).stream()
                .filter(row->id.equals(row.snapshotId())).findFirst().orElseThrow().status());
        assertEquals("RETAINED_VALID",
                retention.inspectExpiredArtifactsForWorkspace(family,10)
                        .stream().filter(row->id.equals(row.snapshotId()))
                        .findFirst().orElseThrow().status());
        assertTrue(retention.inspectExpiredArtifactsForWorkspace(family+999999L,10).isEmpty());
        assertThrows(IllegalArgumentException.class,
                ()->retention.inspectExpiredArtifactsForWorkspace(-1,10));
        assertThrows(IllegalArgumentException.class,
                ()->retention.inspectExpiredArtifactsForWorkspace(family,101));
        // RET-02: dry-run classification never modifies registry or ciphertext.
        db.update("UPDATE workspace_protective_backups SET storage_key=? WHERE snapshot_id=?",
                "local:v1:wrong/"+id+".fpbackup",id);
        assertEquals("BLOCK_STORAGE_KEY",retention.inspectExpiredArtifacts(10).stream()
                .filter(row->id.equals(row.snapshotId())).findFirst().orElseThrow().status());
        // V51 CHECK is artifact-evidence based, not EXPIRED-state based:
        // populated artifact_bytes/artifact_stored_at require storage_key.
        // Nulling the key must fail even after terminal expiry.
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->db.update(
                        "UPDATE workspace_protective_backups SET storage_key=NULL WHERE snapshot_id=?",id));
        assertEquals("BLOCK_STORAGE_KEY",retention.inspectExpiredArtifacts(10).stream()
                .filter(row->id.equals(row.snapshotId())).findFirst().orElseThrow().status());
        db.update("UPDATE workspace_protective_backups SET storage_key=?,artifact_sha256=? WHERE snapshot_id=?",
                "local:v1:"+family+"/"+id+".fpbackup","f".repeat(64),id);
        assertEquals("BLOCK_INTEGRITY_OR_MISSING",retention.inspectExpiredArtifacts(10).stream()
                .filter(row->id.equals(row.snapshotId())).findFirst().orElseThrow().status());
        assertEquals(1,db.update(
                "UPDATE workspace_protective_backups SET artifact_sha256=NULL WHERE snapshot_id=?",id));
        assertEquals("BLOCK_DIGEST",retention.inspectExpiredArtifacts(10).stream()
                .filter(row->id.equals(row.snapshotId())).findFirst().orElseThrow().status());
        db.update("UPDATE workspace_protective_backups SET artifact_sha256=? WHERE snapshot_id=?",
                stored.sha256(),id);
        assertEquals("RETAINED_VALID",retention.inspectExpiredArtifacts(10).stream()
                .filter(row->id.equals(row.snapshotId())).findFirst().orElseThrow().status());
        assertTrue(Files.isRegularFile(artifact));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->tx.execute(status->retention.acquireVerifiedLease(
                        family,id,java.time.Duration.ofMinutes(5))));
        assertTrue(Files.isRegularFile(artifact),"Lease denial and replay never unlink");
        assertEquals(1,db.update("DELETE FROM workspace_protective_backups WHERE snapshot_id=?",id));
    }


    @Test void competingWorkersSkipLockedRowAndRetryAfterRelease() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var manager=new DataSourceTransactionManager(ds);
        var tx=new TransactionTemplate(manager);
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Retention worker race') RETURNING id",Long.class);
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
                     schema_version,state,created_at,expires_at)
                VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,
                        'V50','REQUESTED',NOW()-INTERVAL '40 days',
                        NOW()-INTERVAL '1 day')
                """,id,family,UUID.randomUUID(),member,"a".repeat(64));
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        var retention=new WorkspaceBackupRetentionService(db,store);
        var locked=new java.util.concurrent.CountDownLatch(1);
        var release=new java.util.concurrent.CountDownLatch(1);
        var workers=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var holder=workers.submit(()->tx.execute(status->{
                db.queryForList("""
                    SELECT id FROM workspace_protective_backups
                    WHERE workspace_id=? AND snapshot_id=? FOR UPDATE
                    """,family,id);
                locked.countDown();
                try {
                    if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))
                        throw new IllegalStateException("RETENTION_TEST_LOCK_TIMEOUT");
                } catch(InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return true;
            }));
            assertTrue(locked.await(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(0, (int)tx.execute(status->retention.expireEligible(1)),
                    "SKIP LOCKED must not wait for a concurrently held row");
            assertEquals("REQUESTED",db.queryForObject(
                    "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                    String.class,id));
            release.countDown();
            assertTrue(holder.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, (int)tx.execute(status->retention.expireEligible(1)));
            assertEquals(0, (int)tx.execute(status->retention.expireEligible(1)));
            assertEquals("EXPIRED",db.queryForObject(
                    "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                    String.class,id));
            verifyNoInteractions(store);
        } finally {
            release.countDown();
            workers.shutdownNow();
            db.update("DELETE FROM workspace_protective_backups WHERE snapshot_id=?",id);
        }
    }

    @Test void ciphertextIntegrityFailureRollsBackSqlWithoutUnlink() {
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
        doThrow(new SecurityException("BACKUP_FILE_MISSING_OR_UNSAFE"))
                .when(storage).verifyStoredCiphertext(eq(family),eq(id),anyString());
        var retention=new WorkspaceBackupRetentionService(db,storage);
        assertEquals("BACKUP_FILE_MISSING_OR_UNSAFE",assertThrows(SecurityException.class,
                ()->tx.execute(status->retention.expireEligible(1))).getMessage());
        assertEquals("BACKING_UP",db.queryForObject(
                "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                String.class,id));
        verify(storage).verifyStoredCiphertext(eq(family),eq(id),anyString());
        verify(storage,never()).deleteAuthorizedExpired(anyLong(),any());
        // SQL CHECK already rejects partial evidence before retention sees it.
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->db.update("UPDATE workspace_protective_backups SET storage_key=NULL WHERE snapshot_id=?",id));
        assertEquals("BACKING_UP",db.queryForObject(
                "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                String.class,id));
        assertEquals(1,db.update(
                "DELETE FROM workspace_protective_backups WHERE snapshot_id=?",id));
    }
}
