package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspaceBackupReconciliationAssessmentPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("orphan_reconciliation")
            .withUsername("audit_test").withPassword("audit_test");
    @TempDir Path temp;

    @Test void actualFilesystemRestartEvidenceDoesNotOverrideActiveLeaseOrRegistry() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        long a=jdbc.queryForObject(
                "INSERT INTO families(name) VALUES ('Recovery A') RETURNING id",Long.class);
        long b=jdbc.queryForObject(
                "INSERT INTO families(name) VALUES ('Recovery B') RETURNING id",Long.class);
        long role=jdbc.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                        "VALUES (?,true,'PARENT','Parent') RETURNING id",Long.class,a);
        long member=jdbc.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) " +
                        "VALUES (?,?,true,'PARENT','Parent') RETURNING id",Long.class,a,role);

        var portable=new ProtectiveBackupPortableFile();
        var store=new WorkspaceBackupLocalArtifactStore(temp.resolve("archive").toString(),portable);
        UUID registered=UUID.randomUUID(),orphan=UUID.randomUUID(),lease=UUID.randomUUID();
        store.store(a,registered,encrypted(portable,a,registered));
        store.store(a,orphan,encrypted(portable,a,orphan));
        Path folder=temp.resolve("archive").resolve(Long.toString(a));
        Files.write(folder.resolve(".upload-crashed.tmp"),new byte[]{1,2,3});
        Instant now=Instant.now();
        FileTime old=FileTime.from(now.minusSeconds(48*3600L));
        Files.setLastModifiedTime(folder.resolve(registered+".fpbackup"),old);
        Files.setLastModifiedTime(folder.resolve(orphan+".fpbackup"),old);
        Files.setLastModifiedTime(folder.resolve(".upload-crashed.tmp"),old);

        jdbc.update("""
                INSERT INTO workspace_protective_backups
                 (snapshot_id,workspace_id,operation_id,requested_by_member_id,
                  data_epoch,reset_mode,selected_categories_json,source_digest,schema_version,
                  state,created_at,expires_at)
                VALUES (?,?,?, ?,0,'SELECTIVE','["MEMBER_GROUPS"]',?,'V50',
                        'BACKING_UP',NOW()-INTERVAL '31 days',NOW()+INTERVAL '1 day')
                """,registered,a,UUID.randomUUID(),member,"a".repeat(64));
        jdbc.update("""
                INSERT INTO workspace_backup_restore_leases
                 (lease_id,snapshot_id,workspace_id,expires_at)
                VALUES (?,?,?,NOW()+INTERVAL '5 minutes')
                """,lease,registered,a);

        var assessment=new WorkspaceBackupReconciliationAssessmentService(
                jdbc,store,Clock.fixed(now,java.time.ZoneOffset.UTC));
        var found=assessment.assess(a,10).stream().collect(Collectors.toMap(
                WorkspaceBackupReconciliationAssessmentService.Assessment::fileName,
                x->x));
        assertEquals("ACTIVE_RESTORE_LEASE_HOLD",found.get(registered+".fpbackup").classification());
        assertEquals("UNREGISTERED_FILE_REVIEW_ONLY",found.get(orphan+".fpbackup").classification());
        assertEquals("STALE_TEMP_UPLOAD_REVIEW_ONLY",found.get(".upload-crashed.tmp").classification());
        assertTrue(found.values().stream().noneMatch(
                WorkspaceBackupReconciliationAssessmentService.Assessment::deleteAuthorized));
        assertTrue(Files.exists(folder.resolve(orphan+".fpbackup")));
        assertTrue(Files.exists(folder.resolve(".upload-crashed.tmp")));

        jdbc.update("DELETE FROM workspace_backup_restore_leases WHERE lease_id=?",lease);
        assertEquals("IN_PROGRESS_REGISTRY_HOLD",
                assessment.assess(a,10).stream().filter(x->registered.equals(x.snapshotId()))
                        .findFirst().orElseThrow().classification());
        jdbc.update("UPDATE workspace_protective_backups SET state='EXPIRED' WHERE snapshot_id=?",registered);
        assertEquals("REGISTRY_OWNED_FILE_HOLD",
                assessment.assess(a,10).stream().filter(x->registered.equals(x.snapshotId()))
                        .findFirst().orElseThrow().classification());
        assertEquals(1L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",Long.class,a));
        assertEquals(0L,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",Long.class,b));
    }

    private static byte[] encrypted(ProtectiveBackupPortableFile portable,long family,UUID id)
            throws Exception {
        var gen=KeyGenerator.getInstance("AES");
        gen.init(256);
        byte[] payload="{\"format\":\"test\",\"categories\":{}}".getBytes(StandardCharsets.UTF_8);
        return portable.encode(new ProtectiveBackupArtifactCodec().seal(
                id,family,UUID.randomUUID(),0L,"a".repeat(64),"V50",payload,gen.generateKey()));
    }
}
