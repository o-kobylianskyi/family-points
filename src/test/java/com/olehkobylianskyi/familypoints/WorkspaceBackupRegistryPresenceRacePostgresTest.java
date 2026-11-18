package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Reverse audit TOCTOU: disk integrity evidence is never a durable registry permit. */
@Testcontainers
class WorkspaceBackupRegistryPresenceRacePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("reverse_orphan_race")
            .withUsername("reverse_audit").withPassword("reverse_audit");

    @Test void registryEvidenceChangesDuringCiphertextReadAndNextAuditRefusesStaleDigest()
            throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var otherConnection=new JdbcTemplate(new DriverManagerDataSource(
                DB.getJdbcUrl(),DB.getUsername(),DB.getPassword()));
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Reverse audit A') RETURNING id",Long.class);
        long neighbor=db.queryForObject("INSERT INTO families(name) VALUES ('Reverse audit B') RETURNING id",Long.class);
        long role=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,family);
        long member=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) " +
                "VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,family,role);
        UUID snapshot=UUID.randomUUID();
        String firstSha="a".repeat(64),changedSha="b".repeat(64);
        db.update("""
                INSERT INTO workspace_protective_backups
                  (snapshot_id,workspace_id,operation_id,requested_by_member_id,
                   data_epoch,reset_mode,selected_categories_json,source_digest,
                   schema_version,state,storage_key,encrypted,artifact_sha256,
                   artifact_bytes,artifact_stored_at,
                   created_at,expires_at)
                VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,
                        'V50','BACKING_UP',?,TRUE,?,128,NOW(),
                        NOW()-INTERVAL '1 day',NOW()+INTERVAL '31 days')
                """,snapshot,family,UUID.randomUUID(),member,"c".repeat(64),
                "local:v1:"+family+"/"+snapshot+".fpbackup",firstSha);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var started=new CountDownLatch(1);
        var committed=new CountDownLatch(1);
        doAnswer(invocation->{
            started.countDown();
            assertTrue(committed.await(15,TimeUnit.SECONDS));
            return null;
        }).when(storage).verifyStoredCiphertext(family,snapshot,firstSha);
        doThrow(new SecurityException("BACKUP_STORAGE_HASH_MISMATCH"))
                .when(storage).verifyStoredCiphertext(family,snapshot,changedSha);

        var pool=Executors.newSingleThreadExecutor();
        try {
            Future<?> write=pool.submit(()->{
                try {
                    if(!started.await(15,TimeUnit.SECONDS))
                        throw new IllegalStateException("Audit did not start");
                    otherConnection.update("""
                            UPDATE workspace_protective_backups
                            SET artifact_sha256=? WHERE workspace_id=? AND snapshot_id=?
                            """,changedSha,family,snapshot);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                } finally {committed.countDown();}
            });
            var audit=new WorkspaceBackupRegistryPresenceAuditService(db,storage);
            var previous=audit.audit(family,10).get(0);
            write.get(15,TimeUnit.SECONDS);
            // The first read is a stale observation: it must NEVER authorize
            // any restore or delete, regardless of its classification.
            assertEquals("REGISTRY_CHANGED_DURING_AUDIT",previous.classification());
            var current=audit.audit(family,10).get(0);
            assertEquals("REGISTRY_CIPHERTEXT_INVALID",current.classification());
            assertEquals(changedSha,db.queryForObject(
                    "SELECT artifact_sha256 FROM workspace_protective_backups WHERE snapshot_id=?",
                    String.class,snapshot).trim());
            assertEquals(0L,db.queryForObject(
                    "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",
                    Long.class,neighbor));
            verify(storage,never()).deleteAuthorizedExpired(anyLong(),any());
        } finally {pool.shutdownNow();}
    }

    @Test void zeroOrUnboundedRequestNeverReadsRegistryOrStorage() {
        var db=mock(JdbcTemplate.class);
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        var audit=new WorkspaceBackupRegistryPresenceAuditService(db,store);
        assertThrows(IllegalArgumentException.class,()->audit.audit(0L,10));
        assertThrows(IllegalArgumentException.class,()->audit.audit(1L,0));
        assertThrows(IllegalArgumentException.class,()->audit.audit(1L,1001));
        verifyNoInteractions(db,store);
    }
}
