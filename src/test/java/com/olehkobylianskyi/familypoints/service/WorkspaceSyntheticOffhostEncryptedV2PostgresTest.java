package com.olehkobylianskyi.familypoints.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REL-OFFHOST-SYNTHETIC-V2-PG17-TEST-01. Synthetic source only; no host, family
 * credentials, production endpoint, public Restore, or persistent Docker volume.
 *
 * This checks a PAIR of a PostgreSQL custom dump and encrypted original-ancestry
 * V2 bytes from a quiescent single-thread test fixture. It is NOT proof of an
 * operator-controlled write freeze, a real registry lifecycle, off-host transport,
 * financial-history portability or native Restore Execute.
 */
class WorkspaceSyntheticOffhostEncryptedV2PostgresTest {
    @TempDir Path scratch;

    private static long insert(JdbcTemplate jdbc, String sql, Object... values) {
        Long result = jdbc.queryForObject(sql, Long.class, values);
        assertNotNull(result);
        return result;
    }

    private static void ok(org.testcontainers.containers.Container.ExecResult result) {
        assertEquals(0, result.getExitCode(), "Container command failed: " + result.getStderr());
    }

    @Test
    void syntheticPg17DumpAndAuthenticatedV2ArchiveRemainPaired() throws Exception {
        // Both Testcontainers are disposable; the target is a distinct EMPTY DB.
        try (var source = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("source_fixture").withUsername("qa").withPassword("qa");
             var target = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("empty_target").withUsername("qa").withPassword("qa")) {
            source.start();
            target.start();
            var src = new DriverManagerDataSource(
                    source.getJdbcUrl(), source.getUsername(), source.getPassword());
            var dst = new DriverManagerDataSource(
                    target.getJdbcUrl(), target.getUsername(), target.getPassword());
            Flyway.configure().dataSource(src)
                    .locations("classpath:db/migration").load().migrate();
            var jdbc = new JdbcTemplate(src);
            var restored = new JdbcTemplate(dst);

            long family = insert(jdbc,
                    "INSERT INTO families(name) VALUES ('Synthetic source') RETURNING id");
            long role = insert(jdbc, """
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """, family);
            insert(jdbc, """
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Synthetic member',true) RETURNING id
                    """, family, role);
            long epoch = jdbc.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?", Long.class, family);
            assertEquals(1L, jdbc.queryForObject(
                    "SELECT count(*) FROM family_members WHERE family_id=?", Long.class, family));

            byte[] rawKey = new byte[32];
            new SecureRandom().nextBytes(rawKey);
            var key = new SecretKeySpec(rawKey, "AES");
            byte[] wrongKeyBytes = new byte[32];
            new SecureRandom().nextBytes(wrongKeyBytes);
            var wrongKey = new SecretKeySpec(wrongKeyBytes, "AES");
            Arrays.fill(rawKey, (byte) 0);
            Arrays.fill(wrongKeyBytes, (byte) 0);

            var codec = new ProtectiveBackupArtifactCodec();
            var portable = new ProtectiveBackupPortableFile();
            var inspector = new ProtectiveBackupImportInspector(portable, codec);
            var exporter = new WorkspacePortableAncestryV2Exporter(jdbc, codec, portable);
            var tx = new TransactionTemplate(new DataSourceTransactionManager(src));
            tx.setReadOnly(true);
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            UUID snapshot = UUID.randomUUID();
            UUID operation = UUID.randomUUID();

            // Only the test thread can write this fixture. Capture starts after
            // all setup writes and ends before any further source modifications.
            Instant freezeStarted = Instant.now();
            var exported = tx.execute(status ->
                    exporter.export(family, snapshot, operation, epoch, key));
            assertNotNull(exported);
            assertEquals(1, exported.roleCount());
            assertEquals(1, exported.memberCount());
            byte[] encrypted = exported.encryptedFile();
            String archiveSha = ProtectiveBackupArtifactCodec.sha(encrypted);

            var sourceArchive = scratch.resolve("source").resolve(snapshot + ".fpbackup");
            Files.createDirectories(sourceArchive.getParent());
            Files.write(sourceArchive, encrypted);
            ok(source.execInContainer("pg_dump", "-U", "qa", "-d",
                    "source_fixture", "-Fc", "-f", "/tmp/paired.dump"));
            Path dump = scratch.resolve("paired.dump");
            source.copyFileFromContainer("/tmp/paired.dump", dump.toString());
            byte[] dumpBytes = Files.readAllBytes(dump);
            assertTrue(dumpBytes.length > 0);
            String dumpSha = ProtectiveBackupArtifactCodec.sha(dumpBytes);
            assertEquals(epoch.longValue(), jdbc.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?", Long.class, family));
            assertEquals(1L, jdbc.queryForObject(
                    "SELECT count(*) FROM family_members WHERE family_id=?", Long.class, family));
            Instant freezeEnded = Instant.now();
            assertFalse(freezeEnded.isBefore(freezeStarted));

            // A distinct filesystem directory acts as an isolated "off-host"
            // simulation; actual remote transport is NOT exercised.
            Path remote = scratch.resolve("isolated-target");
            Files.createDirectories(remote);
            Path remoteArchive = remote.resolve(snapshot + ".fpbackup");
            Path remoteDump = remote.resolve("paired.dump");
            Files.copy(sourceArchive, remoteArchive);
            Files.copy(dump, remoteDump);
            assertEquals(archiveSha, ProtectiveBackupArtifactCodec.sha(Files.readAllBytes(remoteArchive)));
            assertEquals(dumpSha, ProtectiveBackupArtifactCodec.sha(Files.readAllBytes(remoteDump)));

            // pg_restore must import the full custom archive, not merely read TOC.
            target.copyFileToContainer(Transferable.of(Files.readAllBytes(remoteDump)),
                    "/tmp/paired.dump");
            ok(target.execInContainer("pg_restore", "-U", "qa", "-d",
                    "empty_target", "--no-owner", "--no-privileges", "/tmp/paired.dump"));
            assertEquals(1L, restored.queryForObject(
                    "SELECT count(*) FROM families WHERE id=?", Long.class, family));
            assertEquals(1L, restored.queryForObject(
                    "SELECT count(*) FROM family_roles WHERE family_id=?", Long.class, family));
            assertEquals(1L, restored.queryForObject(
                    "SELECT count(*) FROM family_members WHERE family_id=?", Long.class, family));
            assertEquals(epoch.longValue(), restored.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?", Long.class, family));
            assertEquals(53, restored.queryForObject(
                    "SELECT max(installed_rank) FROM flyway_schema_history WHERE success", Integer.class));

            var verified = inspector.inspectAncestryV2(
                    Files.readAllBytes(remoteArchive), family, key);
            assertEquals(snapshot, verified.snapshotId());
            assertEquals(epoch.longValue(), verified.sourceEpoch());
            assertEquals(1, verified.memberCount());
            assertEquals(1, verified.roleCount());
            assertFalse(verified.restorable());

            // All negative tests are offline and do not mutate either database.
            assertThrows(RuntimeException.class,
                    () -> inspector.inspectAncestryV2(encrypted, family, wrongKey));
            assertThrows(RuntimeException.class,
                    () -> inspector.inspectAncestryV2(encrypted, family + 1, key));
            byte[] tampered = encrypted.clone();
            tampered[tampered.length - 1] ^= 1;
            assertThrows(RuntimeException.class,
                    () -> inspector.inspectAncestryV2(tampered, family, key));
            var manifest = portable.decode(encrypted).manifest();
            var alteredEpoch = new ProtectiveBackupArtifactCodec.Manifest(
                    manifest.snapshotId(), manifest.workspaceId(), manifest.operationId(),
                    manifest.dataEpoch() + 1, manifest.sourceDigest(),
                    manifest.schemaVersion(), manifest.createdAt(),
                    manifest.contentSha256(), manifest.contentBytes());
            var changed = new ProtectiveBackupArtifactCodec.EncryptedArtifact(
                    alteredEpoch, portable.decode(encrypted).ciphertext());
            assertThrows(RuntimeException.class,
                    () -> inspector.inspectAncestryV2(portable.encode(changed), family, key));
            assertNotEquals("deadbeef", archiveSha);
            assertEquals(dumpSha, ProtectiveBackupArtifactCodec.sha(Files.readAllBytes(remoteDump)));
        }
    }
}
