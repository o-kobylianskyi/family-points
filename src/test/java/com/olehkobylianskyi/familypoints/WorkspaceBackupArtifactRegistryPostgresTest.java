package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * V51 lifecycle evidence on an isolated disposable PostgreSQL instance.
 * Neither a staged hash nor a valid file is database recovery verification.
 */
@Testcontainers
class WorkspaceBackupArtifactRegistryPostgresTest {
    @Container static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("artifact_registry").withUsername("artifact")
            .withPassword("artifact_test_only");

    @Test void stagedFileEvidenceIsDurableTenantScopedAndCannotBeReusedOrPromoted() {
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(source);
        long a=jdbc.queryForObject("INSERT INTO families(name) VALUES ('Artifact A') RETURNING id",Long.class);
        long b=jdbc.queryForObject("INSERT INTO families(name) VALUES ('Artifact B') RETURNING id",Long.class);
        long role=jdbc.queryForObject("""
                INSERT INTO family_roles(family_id,system_role,code,name)
                VALUES (?,true,'PARENT','Parent') RETURNING id
                """,Long.class,a);
        long member=jdbc.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,active,member_type,name)
                VALUES (?,?,true,'PARENT','Artifact Parent') RETURNING id
                """,Long.class,a,role);
        UUID snapshot=UUID.randomUUID(),operation=UUID.randomUUID();
        jdbc.update("""
                INSERT INTO workspace_protective_backups
                  (snapshot_id,workspace_id,operation_id,requested_by_member_id,data_epoch,
                   reset_mode,selected_categories_json,source_digest,schema_version)
                VALUES (?,?,?,?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,'V51')
                """,snapshot,a,operation,member,"a".repeat(64));

        var registry=new WorkspaceProtectiveBackupService(jdbc,
                mock(WorkspaceEpochTransactionGuard.class),mock(WorkspaceResetSnapshotService.class));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->registry.recordStaged(b,snapshot,operation,"a".repeat(64),"b".repeat(64),120));
        registry.recordStaged(a,snapshot,operation,"a".repeat(64),"b".repeat(64),120);
        var row=jdbc.queryForMap("""
                SELECT state,storage_key,artifact_sha256,artifact_bytes,
                       artifact_stored_at,restore_verified_at,verified_at
                FROM workspace_protective_backups WHERE workspace_id=? AND snapshot_id=?
                """,a,snapshot);
        assertEquals("BACKING_UP",row.get("state"));
        assertEquals("local:v1:"+a+"/"+snapshot+".fpbackup",row.get("storage_key"));
        assertEquals("b".repeat(64),row.get("artifact_sha256"));
        assertEquals(120L,((Number)row.get("artifact_bytes")).longValue());
        assertNotNull(row.get("artifact_stored_at"));
        assertNull(row.get("restore_verified_at"));
        assertNull(row.get("verified_at"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->registry.recordStaged(a,snapshot,operation,"a".repeat(64),"c".repeat(64),120));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("""
                UPDATE workspace_protective_backups SET state='VERIFIED'
                WHERE workspace_id=? AND snapshot_id=?
                """,a,snapshot));
        assertEquals("BACKING_UP",jdbc.queryForObject("""
                SELECT state FROM workspace_protective_backups WHERE workspace_id=? AND snapshot_id=?
                """,String.class,a,snapshot));
        assertEquals(0L,jdbc.queryForObject("""
                SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?
                """,Long.class,b));
    }
}
