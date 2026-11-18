package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Disposable PG18 proof that a file scan is never a transactional delete permit. */
@Testcontainers
class WorkspaceBackupReconciliationRacePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("orphan_race")
            .withUsername("orphan_race").withPassword("orphan_race");

    @Test void registryCommitBetweenFileInventoryAndDbLookupRevokesOrphanClassification() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var reader=new JdbcTemplate(ds);
        var writer=new JdbcTemplate(new DriverManagerDataSource(
                DB.getJdbcUrl(),DB.getUsername(),DB.getPassword()));
        long family=reader.queryForObject("INSERT INTO families(name) VALUES ('Race A') RETURNING id",Long.class);
        long other=reader.queryForObject("INSERT INTO families(name) VALUES ('Race B') RETURNING id",Long.class);
        long role=reader.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,family);
        long member=reader.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) " +
                "VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,family,role);
        UUID snapshot=UUID.randomUUID();
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        Instant now=Instant.parse("2026-11-18T12:00:00Z");
        var inventoryStarted=new CountDownLatch(1);
        var writerDone=new CountDownLatch(1);
        when(store.inventory(family,10)).thenAnswer(invocation->{
            inventoryStarted.countDown();
            if(!writerDone.await(15,TimeUnit.SECONDS))
                throw new IllegalStateException("Race writer timeout");
            return List.of(new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                    snapshot+".fpbackup",snapshot,128L,true,false,now.minusSeconds(172800)));
        });

        var pool=Executors.newSingleThreadExecutor();
        try {
            Future<?> future=pool.submit(()->{
                try {
                    if(!inventoryStarted.await(15,TimeUnit.SECONDS))
                        throw new IllegalStateException("Inventory did not start");
                    writer.update("""
                            INSERT INTO workspace_protective_backups
                              (snapshot_id,workspace_id,operation_id,requested_by_member_id,
                               data_epoch,reset_mode,selected_categories_json,source_digest,
                               schema_version,state)
                            VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,
                                    'V50','BACKING_UP')
                            """,snapshot,family,UUID.randomUUID(),member,"a".repeat(64));
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                } finally {writerDone.countDown();}
            });
            var audit=new WorkspaceBackupReconciliationAssessmentService(
                    reader,store,Clock.fixed(now,ZoneOffset.UTC));
            var result=audit.assess(family,10);
            future.get(15,TimeUnit.SECONDS);
            assertEquals(1,result.size());
            assertEquals("IN_PROGRESS_REGISTRY_HOLD",result.get(0).classification());
            assertFalse(result.get(0).deleteAuthorized());
            verify(store,never()).deleteAuthorizedExpired(anyLong(),any());
            assertEquals(1L,reader.queryForObject(
                    "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",
                    Long.class,family));
            assertEquals(0L,reader.queryForObject(
                    "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",
                    Long.class,other));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test void staleUnregisteredObservationCannotAuthorizeLaterDeletionAfterRegistryAppears()
            throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Race C') RETURNING id",Long.class);
        long role=db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,family);
        long member=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) " +
                "VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,family,role);
        UUID snapshot=UUID.randomUUID();
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        Instant now=Instant.parse("2026-11-18T12:00:00Z");
        when(store.inventory(family,10)).thenReturn(List.of(
                new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                        snapshot+".fpbackup",snapshot,128L,true,false,now.minusSeconds(172800))));
        var audit=new WorkspaceBackupReconciliationAssessmentService(
                db,store,Clock.fixed(now,ZoneOffset.UTC));
        var oldResult=audit.assess(family,10).get(0);
        assertEquals("UNREGISTERED_FILE_REVIEW_ONLY",oldResult.classification());
        assertFalse(oldResult.deleteAuthorized());

        db.update("""
                INSERT INTO workspace_protective_backups
                  (snapshot_id,workspace_id,operation_id,requested_by_member_id,
                   data_epoch,reset_mode,selected_categories_json,source_digest,
                   schema_version,state)
                VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,'V50','BACKING_UP')
                """,snapshot,family,UUID.randomUUID(),member,"a".repeat(64));
        var freshResult=audit.assess(family,10).get(0);
        assertEquals("IN_PROGRESS_REGISTRY_HOLD",freshResult.classification());
        assertFalse(freshResult.deleteAuthorized());
        assertFalse(oldResult.deleteAuthorized(),"Old scan results must never grant delete rights");
        verify(store,never()).deleteAuthorizedExpired(anyLong(),any());
    }
}
