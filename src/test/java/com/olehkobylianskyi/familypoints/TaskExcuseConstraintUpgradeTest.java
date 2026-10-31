package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class TaskExcuseConstraintUpgradeTest {
    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("excuse_migration_test")
            .withUsername("excuse_test")
            .withPassword("excuse_test_password");

    private Flyway flyway(boolean throughV37) {
        var config = Flyway.configure()
                .dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:db/migration");
        if (throughV37) config.target(MigrationVersion.fromVersion("37"));
        return config.load();
    }

    @Test
    void v38UpgradesLegacyConstraintAndRetainsStrictValidation() throws Exception {
        assertEquals(37, flyway(true).migrate().migrationsExecuted);
        try (var conn = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
             Statement sql = conn.createStatement()) {
            // Re-create the exact missing-enum-value historical schema seen in local PostgreSQL.
            sql.execute("ALTER TABLE task_instances DROP CONSTRAINT task_instances_excuse_reason_check");
            sql.execute("ALTER TABLE task_instances ADD CONSTRAINT task_instances_excuse_reason_check " +
                    "CHECK (excuse_reason IS NULL OR excuse_reason IN " +
                    "('ILLNESS','FAMILY_EVENT','TRAVEL','PARENT_DECISION','OTHER'))");
            sql.execute("INSERT INTO families(name) VALUES ('Excuse regression family')");
        }

        assertEquals(6, flyway(false).migrate().migrationsExecuted);
        try (var conn = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
             Statement sql = conn.createStatement();
             ResultSet result = sql.executeQuery("""
                    SELECT pg_get_constraintdef(oid)
                    FROM pg_constraint
                    WHERE conrelid='public.task_instances'::regclass
                      AND conname='task_instances_excuse_reason_check'
                    """)) {
            assertTrue(result.next());
            String definition = result.getString(1);
            assertTrue(definition.contains("EXCEPTION_PERIOD"), definition);
            assertTrue(definition.contains("ILLNESS"), definition);
        }
        assertEquals(0, flyway(false).migrate().migrationsExecuted);
    }
}
