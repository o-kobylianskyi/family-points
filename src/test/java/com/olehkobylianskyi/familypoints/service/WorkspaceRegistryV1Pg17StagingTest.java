package com.olehkobylianskyi.familypoints.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REL-REGISTRY-V1-PG17-STAGING-01.
 *
 * Genuine V1 REQUESTED -> BACKING_UP registry and encrypted FPB1 staging
 * in disposable PostgreSQL 17 and JUnit temp folders only. Never promotes
 * VERIFIED, executes Restore/Reset, uses actual offhost storage, or claims
 * to test the distinct original-member ancestry V2 exporter.
 */
class WorkspaceRegistryV1Pg17StagingTest {
    @TempDir Path scratch;

    private static long insert(JdbcTemplate db, String sql, Object... arguments) {
        Long id = db.queryForObject(sql, Long.class, arguments);
        assertNotNull(id);
        return id;
    }

    @Test
    void realRegistryAndV1CiphertextRemainTenantEpochAndDigestBound() throws Exception {
        try (var pg = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("registry_v1_synthetic")
                .withUsername("qa").withPassword("qa")) {
            pg.start();
            var ds = new DriverManagerDataSource(
                    pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
            Flyway.configure().dataSource(ds)
                    .locations("classpath:db/migration").load().migrate();
            var db = new JdbcTemplate(ds);
            var manager = new DataSourceTransactionManager(ds);
            var tx = new TransactionTemplate(manager);
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

            assertEquals(17, db.queryForObject(
                    "SELECT current_setting('server_version_num')::integer / 10000", Integer.class));

            long family = insert(db,
                    "INSERT INTO families(name) VALUES ('Synthetic registry A') RETURNING id");
            long neighbor = insert(db,
                    "INSERT INTO families(name) VALUES ('Synthetic registry B') RETURNING id");
            long role = insert(db, """
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """, family);
            long member = insert(db, """
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Synthetic member',true) RETURNING id
                    """, family, role);
            long epoch = db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?", Long.class, family);
            var guard = new WorkspaceEpochTransactionGuard(db);
            var snapshots = new WorkspaceResetSnapshotService(db, guard);
            var registry = new WorkspaceProtectiveBackupService(db, guard, snapshots);
            var codec = new ProtectiveBackupArtifactCodec();
            var format = new ProtectiveBackupPortableFile();
            var inspector = new ProtectiveBackupImportInspector(format, codec);
            var exporter = new WorkspacePortableBackupExporter(db, snapshots, codec, format);
            var storage = new WorkspaceBackupLocalArtifactStore(
                    scratch.resolve("encrypted-v1").toString(), format);
            var staging = new WorkspaceBackupArtifactStagingService(
                    exporter, storage, inspector, format);
            var coordinator = new WorkspaceBackupRegistryStagingCoordinator(
                    registry, staging, storage);

            var categories = List.of("MEMBER_GROUPS");
            String digest = tx.execute(status -> snapshots.digest(family, categories));
            assertNotNull(digest);
            var req = new WorkspaceProtectiveBackupService.Request(
                    UUID.randomUUID(), "SELECTIVE", categories, digest, "V50");
            var keygen = KeyGenerator.getInstance("AES");
            keygen.init(256);
            var key = keygen.generateKey();

            // The coordinator sees a genuine explicit REPEATABLE_READ TX.
            var result = tx.execute(status ->
                    coordinator.registerAndStage(family, member, req, epoch, key));
            assertNotNull(result);
            assertFalse(result.recoveryVerified());
            assertEquals("REQUESTED", result.registryEntry().state());
            assertEquals("STORED_UNVERIFIED", result.artifact().state());
            assertEquals(digest, result.registryEntry().sourceDigest());
            assertEquals(digest, result.artifact().sourceDigest());
            UUID snapshotId = result.registryEntry().snapshotId();
            assertEquals(snapshotId, result.artifact().snapshotId());

            var metadata = db.queryForMap("""
                    SELECT state,storage_key,artifact_sha256,artifact_bytes,encrypted,
                           source_digest,operation_id,data_epoch,restore_verified_at
                    FROM workspace_protective_backups
                    WHERE workspace_id=? AND snapshot_id=?
                    """, family, snapshotId);
            assertEquals("BACKING_UP", metadata.get("state"));
            assertEquals("local:v1:" + family + "/" + snapshotId + ".fpbackup",
                    metadata.get("storage_key"));
            assertEquals(digest, metadata.get("source_digest"));
            assertEquals(req.operationId(), metadata.get("operation_id"));
            assertEquals(epoch, ((Number) metadata.get("data_epoch")).longValue());
            assertEquals(Boolean.TRUE, metadata.get("encrypted"));
            assertNull(metadata.get("restore_verified_at"));
            assertEquals(result.artifact().storageSha256(), metadata.get("artifact_sha256"));
            assertEquals(result.artifact().storedBytes(),
                    ((Number) metadata.get("artifact_bytes")).intValue());
            assertFalse(registry.get(family, snapshotId).restoreVerified());
            assertFalse(registry.get(family, snapshotId).executable());

            Path file = scratch.resolve("encrypted-v1")
                    .resolve(Long.toString(family)).resolve(snapshotId + ".fpbackup");
            assertTrue(Files.isRegularFile(file));
            byte[] original = Files.readAllBytes(file);
            assertEquals(metadata.get("artifact_sha256"),
                    ProtectiveBackupArtifactCodec.sha(original));
            assertEquals(original.length, result.artifact().storedBytes());
            var envelope = format.decode(original).manifest();
            assertEquals(snapshotId, envelope.snapshotId());
            assertEquals(family, envelope.workspaceId());
            assertEquals(req.operationId(), envelope.operationId());
            assertEquals(epoch, envelope.dataEpoch());
            assertEquals("V50", envelope.schemaVersion());
            assertEquals(digest, envelope.sourceDigest());

            var evidence = new WorkspaceBackupRegistryPresenceAuditService(db, storage);
            assertEquals("REGISTRY_CIPHERTEXT_PRESENT",
                    evidence.audit(family, 10).get(0).classification());
            assertTrue(evidence.audit(neighbor, 10).isEmpty());
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(
                    ResponseStatusException.class,
                    () -> registry.get(neighbor, snapshotId)).getStatusCode());

            // Replays and stale source evidence must never publish a second file.
            assertThrows(IllegalStateException.class, () -> tx.execute(status ->
                    coordinator.registerAndStage(family, member, req, epoch, key)));
            var stale = new WorkspaceProtectiveBackupService.Request(
                    UUID.randomUUID(), "SELECTIVE", categories, "a".repeat(64), "V50");
            assertEquals(HttpStatus.CONFLICT, assertThrows(
                    ResponseStatusException.class,
                    () -> tx.execute(status ->
                            registry.request(family, member, stale))).getStatusCode());
            assertEquals(1, storage.inventory(family, 10).size());

            byte[] damaged = original.clone();
            damaged[damaged.length - 1] ^= 1;
            Files.write(file, damaged);
            assertEquals("REGISTRY_CIPHERTEXT_INVALID",
                    evidence.audit(family, 10).get(0).classification());
            Files.write(file, original);
            assertEquals("REGISTRY_CIPHERTEXT_PRESENT",
                    evidence.audit(family, 10).get(0).classification());
            assertEquals("BACKING_UP",
                    db.queryForObject(
                            "SELECT state FROM workspace_protective_backups WHERE snapshot_id=?",
                            String.class, snapshotId));

            // Test cleanup occurs only via @TempDir and container lifecycle.
            // No real registry VERIFIED, offhost copy, or V2 ancestry binding.
        }
    }
}
