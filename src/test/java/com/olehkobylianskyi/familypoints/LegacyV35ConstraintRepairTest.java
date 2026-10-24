package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for a real pre-V35 local database with legacy CHECK constraints removed.
 * Checks the repair callback and immutable V35 on disposable PostgreSQL, never user data.
 */
@Testcontainers
class LegacyV35ConstraintRepairTest {
    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("family_points_legacy_constraint")
            .withUsername("constraint_test")
            .withPassword("constraint_test_password");

    private Flyway flyway(Integer target) {
        var config = Flyway.configure()
                .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:db/migration");
        if (target != null) config.target(MigrationVersion.fromVersion(Integer.toString(target)));
        return config.load();
    }

    private long count(String sql) throws Exception {
        try (var conn = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
             var statement = conn.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    @Test
    void repairsMissingConstraintsWithoutChangingAppliedFlywayChecksums() throws Exception {
        assertEquals(34, flyway(34).migrate().migrationsExecuted);
        try (var conn = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
             Statement statement = conn.createStatement()) {
            statement.execute("ALTER TABLE public.point_transactions DROP CONSTRAINT point_transactions_source_type_check");
            statement.execute("ALTER TABLE public.group_point_transactions DROP CONSTRAINT group_point_transactions_source_type_check");
        }
        assertEquals(0, count("""
                SELECT count(*) FROM pg_constraint WHERE conname IN
                ('point_transactions_source_type_check','group_point_transactions_source_type_check')
                """));
        // Flyway beforeMigrate.sql restores missing names; V35 replaces placeholders
        // with strict REWARD_PURCHASE-compatible checks, followed by V36 and V37.
        assertEquals(3, flyway(null).migrate().migrationsExecuted);
        assertEquals(2, count("""
                SELECT count(*) FROM pg_constraint
                WHERE conname IN ('point_transactions_source_type_check',
                                  'group_point_transactions_source_type_check')
                AND pg_get_constraintdef(oid) LIKE '%REWARD_PURCHASE%'
                """));
        assertEquals(3, count("""
                SELECT count(*) FROM public.flyway_schema_history
                WHERE version IN ('35','36','37') AND success
                """));
        assertEquals(0, flyway(null).migrate().migrationsExecuted);
    }
}
