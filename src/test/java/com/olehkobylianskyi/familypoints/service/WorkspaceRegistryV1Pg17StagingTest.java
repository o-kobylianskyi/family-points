package com.olehkobylianskyi.familypoints.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REL-REGISTRY-V1-PG17-STAGING-01: disposable PostgreSQL17, V1 FPB1 only.
 * Real transactional registry REQUESTED -> BACKING_UP + file evidence.
 * Not V2 ancestry, verified restore, offhost copy, or production permission.
 */
class WorkspaceRegistryV1Pg17StagingTest {
    @TempDir Path temp;

    @Test
    void stagesRealV1CiphertextAndRejectsRegistryDriftWithoutRestore() throws Exception {
        try (var pg = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("registry_v1_synthetic")
                .withUsername("qa").withPassword("qa")) {
            pg.start();
            var ds = new DriverManagerDataSource(pg.getJdbcUrl(),
                    pg.getUsername(), pg.getPassword());
            Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                    .load().migrate();
            var db = new JdbcTemplate(ds);
            var manager = new DataSourceTransactionManager(ds);
            var tx = new TransactionTemplate(manager);
            long workspace = db.queryForObject(
                    "INSERT INTO families(name) VALUES ('Synthetic V1') RETURNING id", Long.class);
            long foreign = db.queryForObject(
                    "INSERT INTO families(name) VALUES ('Unrelated synthetic') RETURNING id", Long.class);
            long role = db.queryForObject("""
                    INSERT INTO family_roles(family_id,code,name,system_role)
                    VALUES (?,'PARENT','Parent',true) RETURNING id
                    """, Long.class, workspace);
            long member = db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                    VALUES (?,?,'Synthetic parent',true,'PARENT') RETURNING id
                    """, Long.class, workspace, role);
            assertEquals(0L, db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?", Long.class, workspace));

            var guard = new WorkspaceEpochTransactionGuard(db);
            var snapshots = new WorkspaceResetSnapshotService(db, guard);
            var registry = new WorkspaceProtectiveBackupService(db, guard, snapshots);
            var codec = new ProtectiveBackupArtifactCodec();
            var format = new ProtectiveBackupPortableFile();
            var store = new WorkspaceBackupLocalArtifactStore(temp.resolve("archive").toString(), format);
            var inspector = new ProtectiveBackupImportInspector(format, codec);
            var exporter = new WorkspacePortableBackupExporter(db, snapshots, codec, format);
            var staging = new WorkspaceBackupArtifactStagingService(
                    exporter, store, inspector, format);
            var target = new WorkspaceBackupRegistryStagingCoordinator(
                    registry, staging, store);

            // Invoking the actual Spring transaction interceptor is essential.
            var proxyFactory = new ProxyFactory(target);
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new TransactionInterceptor(
                    manager, new AnnotationTransactionAttributeSource()));
            var coordinator = (WorkspaceBackupRegistryStagingCoordinator) proxyFactory.getProxy();
            var generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            var disposableKey = generator.generateKey();
            var selected = List.of("MEMBER_GROUPS");
            String digest = tx.execute(status -> snapshots.digest(workspace, selected));
            UUID operation = UUID.randomUUID();
            var request = new WorkspaceProtectiveBackupService.Request(
                    operation, "SELECTIVE", selected, digest, "V50");

            var result = coordinator.registerAndStage(workspace, member,
                    request, 0L, disposableKey);
            UUID id = result.artifact().snapshotId();
            assertEquals(id, result.registryEntry().snapshotId());
            assertEquals(workspace, result.registryEntry().workspaceId());
            assertEquals(operation, result.registryEntry().operationId());
            assertEquals("REQUESTED", result.registryEntry().state());
            assertEquals("STORED_UNVERIFIED", result.artifact().state());
            assertFalse(result.recoveryVerified());

            var rows = db.queryForList("""
                    SELECT state,workspace_id,operation_id,data_epoch,
                           source_digest,storage_key,artifact_sha256,
                           artifact_bytes,encrypted,restore_verified_at
                    FROM workspace_protective_backups
                    WHERE workspace_id=? AND snapshot_id=?
                    """, workspace, id);
            assertEquals(1, rows.size());
            var row = rows.get(0);
            assertEquals("BACKING_UP", row.get("state"));
            assertEquals(workspace, ((Number)row.get("workspace_id")).longValue());
            assertEquals(operation, row.get("operation_id"));
            assertEquals(0L, ((Number)row.get("data_epoch")).longValue());
            assertEquals(digest, row.get("source_digest"));
            assertEquals("local:v1:" + workspace + "/" + id + ".fpbackup",
                    row.get("storage_key"));
            assertEquals(Boolean.TRUE, row.get("encrypted"));
            assertNull(row.get("restore_verified_at"));

            Path encrypted = temp.resolve("archive").resolve(Long.toString(workspace))
                    .resolve(id + ".fpbackup");
            assertTrue(Files.isRegularFile(encrypted));
            byte[] encryptedBytes = Files.readAllBytes(encrypted);
            String sha = ProtectiveBackupArtifactCodec.sha(encryptedBytes);
            assertEquals(sha, row.get("artifact_sha256"));
            assertEquals(encryptedBytes.length,
                    ((Number)row.get("artifact_bytes")).intValue());
            assertEquals(sha, result.artifact().storageSha256());
            var manifest = format.decode(encryptedBytes).manifest();
            assertEquals(id, manifest.snapshotId());
            assertEquals(operation, manifest.operationId());
            assertEquals(workspace, manifest.workspaceId());
            assertEquals(0, manifest.dataEpoch());
            assertEquals(digest, manifest.sourceDigest());
            var proof = inspector.inspect(encryptedBytes, workspace, disposableKey);
            assertFalse(proof.restorable());
            assertEquals(id, proof.snapshotId());
            assertEquals(selected, proof.availableCategories());

            var audit = new WorkspaceBackupRegistryPresenceAuditService(db, store);
            assertEquals("REGISTRY_CIPHERTEXT_PRESENT",
                    audit.audit(workspace, 10).get(0).classification());
            assertTrue(audit.audit(foreign, 10).isEmpty());
            assertEquals("BACKING_UP", registry.get(workspace, id).state());
            assertFalse(registry.get(workspace, id).restoreVerified());
            assertFalse(registry.get(workspace, id).executable());

            // A registry row with a wrong SHA must fail a READ-ONLY audit.
            db.update("""
                    UPDATE workspace_protective_backups
                    SET artifact_sha256=? WHERE workspace_id=? AND snapshot_id=?
                    """, "f".repeat(64), workspace, id);
            assertEquals("REGISTRY_CIPHERTEXT_INVALID",
                    audit.audit(workspace, 10).get(0).classification());
            assertEquals(sha, ProtectiveBackupArtifactCodec.sha(
                    Files.readAllBytes(encrypted)));
            db.update("""
                    UPDATE workspace_protective_backups
                    SET artifact_sha256=? WHERE workspace_id=? AND snapshot_id=?
                    """, sha, workspace, id);
            assertEquals("REGISTRY_CIPHERTEXT_PRESENT",
                    audit.audit(workspace, 10).get(0).classification());

            // Replay cannot publish another file or promote the registry.
            assertThrows(RuntimeException.class, () -> coordinator.registerAndStage(
                    workspace, member, request, 0L, disposableKey));
            assertEquals("BACKING_UP", registry.get(workspace, id).state());
            assertEquals(1L, db.queryForObject(
                    "SELECT count(*) FROM workspace_protective_backups WHERE workspace_id=?",
                    Long.class, workspace));
            assertTrue(Files.isRegularFile(encrypted));
            assertEquals(sha, ProtectiveBackupArtifactCodec.sha(Files.readAllBytes(encrypted)));

            // A fabricated source fingerprint is never accepted by the real registry.
            var stale = new WorkspaceProtectiveBackupService.Request(
                    UUID.randomUUID(), "SELECTIVE", selected, "a".repeat(64), "V50");
            assertThrows(RuntimeException.class, () -> tx.execute(status ->
                    registry.request(workspace, member, stale)));
            assertTrue(registry.list(foreign, 10).isEmpty());
        }
    }
}
