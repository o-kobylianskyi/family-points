package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Disposable PostgreSQL 17 integration proof for an existing V52 workspace
 * upgraded through immutable V53 Restore + V54 reward operation receipts.
 *
 * This is intentionally not production/offhost or an executable RESTORE test.
 */
@Testcontainers
class IntegratedV53V54LegacyPg17UpgradeTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("family_points")
            .withUsername("family_points")
            .withPassword("synthetic_pg17_only");

    @Test
    void populatedV52UpgradesInOrderWithoutBackfillingOwnerOrLosingFamilyData() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var db = new JdbcTemplate(dataSource);
        assertEquals(17, db.queryForObject(
                "SELECT current_setting('server_version_num')::integer / 10000", Integer.class));

        var initial = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("52")).load();
        assertEquals(52, initial.migrate().migrationsExecuted);

        Long familyId = db.queryForObject(
                "INSERT INTO families(name) VALUES ('Synthetic existing V52 family') RETURNING id", Long.class);
        Long roleId = db.queryForObject(
                "INSERT INTO family_roles(family_id,system_role,code,name) " +
                        "VALUES (?,true,'PARENT','Original synthetic role') RETURNING id",
                Long.class, familyId);
        Long memberId = db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,active,member_type,name) " +
                        "VALUES (?,?,true,'PARENT','Existing synthetic member') RETURNING id",
                Long.class, familyId, roleId);

        assertEquals(0L, db.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version IN ('53','54')", Long.class));
        var upgraded = Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").load();
        assertEquals(2, upgraded.migrate().migrationsExecuted,
                "Existing V52 database must apply V53 then V54 exactly once");

        assertEquals(2L, db.queryForObject(
                "SELECT count(*) FROM flyway_schema_history " +
                        "WHERE version IN ('53','54') AND success = true AND checksum IS NOT NULL", Long.class));
        assertEquals("53,54", db.queryForObject(
                "SELECT string_agg(version, ',' ORDER BY installed_rank) " +
                        "FROM flyway_schema_history WHERE version IN ('53','54')", String.class));

        assertEquals(1L, db.queryForObject("SELECT count(*) FROM families WHERE id=?", Long.class, familyId));
        assertEquals(1L, db.queryForObject(
                "SELECT count(*) FROM family_roles WHERE id=? AND family_id=?", Long.class, roleId, familyId));
        assertEquals(1L, db.queryForObject(
                "SELECT count(*) FROM family_members WHERE id=? AND family_id=?",
                Long.class, memberId, familyId));

        assertEquals(0L, db.queryForObject(
                "SELECT count(*) FROM workspace_original_owner_bindings WHERE workspace_id=?",
                Long.class, familyId), "V53 must not invent an original owner for a legacy workspace");
        for (String table : new String[]{
                "workspace_restore_permits", "workspace_restore_receipts",
                "reward_purchase_operation_receipts"}) {
            assertEquals(1L, db.queryForObject(
                    "SELECT count(*) FROM information_schema.tables " +
                            "WHERE table_schema='public' AND table_name=?", Long.class, table));
        }
        assertEquals(0L, db.queryForObject(
                "SELECT count(*) FROM reward_purchase_operation_receipts WHERE workspace_id=?",
                Long.class, familyId));
        assertEquals(0, upgraded.migrate().migrationsExecuted,
                "Second migration execution must be a no-op without changed checksums");
    }
}
