package com.olehkobylianskyi.familypoints;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REL-PG17: independent ephemeral PostgreSQL 17 proof.
 * No application API, real accounts, external databases, prod credentials,
 * database restore action or production endpoint is involved.
 */
@SpringBootTest
@Testcontainers
class EphemeralPg17MigrationRestoreTest {
    static final PostgreSQLContainer SOURCE = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("family_points").withUsername("family_points")
            .withPassword("ephemeral_test_only");
    static final PostgreSQLContainer RESTORED = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("family_points").withUsername("family_points")
            .withPassword("ephemeral_test_only");

    static {
        SOURCE.start();
        RESTORED.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", SOURCE::getJdbcUrl);
        r.add("spring.datasource.username", SOURCE::getUsername);
        r.add("spring.datasource.password", SOURCE::getPassword);
        r.add("spring.flyway.baseline-on-migrate", () -> "false");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("security.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString(
                "ephemeral-local-ci-jwt-only-never-used-on-host-1234567890"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Autowired JdbcTemplate sourceDb;

    @Test
    void realFlyway53AndNonemptyPg17DumpRestoreWithRowParity() throws Exception {
        assertTrue(sourceDb.queryForObject("SELECT current_setting('server_version_num')::integer", Integer.class)
                / 10000 == 17, "MUST be PostgreSQL 17, not 18");
        assertEquals(1L, sourceDb.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '53' AND success", Long.class));
        assertEquals(1L, sourceDb.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '51' AND success", Long.class));
        assertEquals(1L, sourceDb.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'workspace_restore_permits'",
                Long.class));
        assertEquals(1L, sourceDb.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'workspace_restore_receipts'",
                Long.class));

        long family = sourceDb.queryForObject(
                "INSERT INTO families(name) VALUES ('Ephemeral PG17 family') RETURNING id", Long.class);
        long role = sourceDb.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) "
                + "VALUES (?,true,'PARENT','Synthetic role') RETURNING id", Long.class, family);
        sourceDb.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) "
                + "VALUES (?,?,true,'PARENT','Synthetic member') RETURNING id", Long.class, family, role);

        long originalFamilies = sourceDb.queryForObject("SELECT count(*) FROM families", Long.class);
        long originalRoles = sourceDb.queryForObject("SELECT count(*) FROM family_roles", Long.class);
        long originalMembers = sourceDb.queryForObject("SELECT count(*) FROM family_members", Long.class);
        assertTrue(originalFamilies > 0 && originalRoles > 0 && originalMembers > 0);

        var restoredDs = new DriverManagerDataSource(
                RESTORED.getJdbcUrl(), RESTORED.getUsername(), RESTORED.getPassword());
        var targetDb = new JdbcTemplate(restoredDs);
        assertEquals(0L, targetDb.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema='public' AND table_type='BASE TABLE'", Long.class),
                "Target must be independently EMPTY before pg_restore");

        Path dump = Files.createTempFile("fp-pg17-ephemeral-", ".dump");
        try {
            Container.ExecResult exported = SOURCE.execInContainer("pg_dump",
                    "-U", "family_points", "-d", "family_points", "-Fc",
                    "-f", "/tmp/ephemeral.dump");
            assertEquals(0, exported.getExitCode(), "pg_dump -Fc failed: " + exported.getStderr());
            SOURCE.copyFileFromContainer("/tmp/ephemeral.dump", dump.toString());
            assertTrue(Files.size(dump) > 100, "Archive must have nonempty payload");
            var checksum = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(dump));
            assertEquals(32, checksum.length);

            RESTORED.copyFileToContainer(MountableFile.forHostPath(dump), "/tmp/ephemeral.dump");
            Container.ExecResult restored = RESTORED.execInContainer("pg_restore",
                    "--exit-on-error", "--no-owner", "--no-acl", "-U", "family_points",
                    "-d", "family_points", "/tmp/ephemeral.dump");
            assertEquals(0, restored.getExitCode(), "REAL pg_restore failed: " + restored.getStderr());

            assertEquals(originalFamilies, targetDb.queryForObject("SELECT count(*) FROM families", Long.class));
            assertEquals(originalRoles, targetDb.queryForObject("SELECT count(*) FROM family_roles", Long.class));
            assertEquals(originalMembers, targetDb.queryForObject("SELECT count(*) FROM family_members", Long.class));
            assertEquals(1L, targetDb.queryForObject(
                    "SELECT count(*) FROM flyway_schema_history WHERE version='53' AND success", Long.class));
            assertEquals(1L, targetDb.queryForObject(
                    "SELECT count(*) FROM flyway_schema_history WHERE version='51' AND success", Long.class));
            assertEquals(0L, targetDb.queryForObject(
                    "SELECT count(*) FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid "
                    + "WHERE t.relname='workspace_restore_permits' AND c.contype='f' AND NOT c.convalidated",
                    Long.class), "Migrated FK constraints must remain validated");
            System.out.printf("EPHEMERAL_PG17_PASS sourceSha/testedSha=%s "
                            + "families=%d roles=%d members=%d restoredFlyway=53 dumpSha256=%s%n",
                    System.getenv().getOrDefault("GITHUB_SHA", "LOCAL_TEST"),
                    originalFamilies, originalRoles, originalMembers,
                    java.util.HexFormat.of().formatHex(checksum));
        } finally {
            Files.deleteIfExists(dump);
        }
    }
}
