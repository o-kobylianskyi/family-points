package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

/** V41 must be an additive audit foundation; no destructive reset is enabled. */
@Testcontainers
class WorkspaceResetAuditSchemaPostgresTest {
    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("workspace_reset_schema")
            .withUsername("workspace_reset_test")
            .withPassword("workspace_reset_test_password");

    @Test void migrationHasScopedReplayKeyAndRestrictiveForeignKeys() {
        Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(new DriverManagerDataSource(
                DB.getJdbcUrl(),DB.getUsername(),DB.getPassword()));

        assertEquals(0, db.queryForObject(
                "SELECT count(*) FROM workspace_reset_operations", Integer.class));
        assertEquals(1, db.queryForObject("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid='workspace_reset_operations'::regclass
                  AND conname='uq_workspace_reset_operation' AND contype='u'
                """, Integer.class));
        assertEquals(2, db.queryForObject("""
                SELECT count(*) FROM pg_constraint
                WHERE conrelid='workspace_reset_operations'::regclass
                  AND contype='f' AND confdeltype='r'
                """, Integer.class));
        assertEquals(0, db.queryForObject("""
                SELECT count(*) FROM workspace_reset_operations
                WHERE status='SUCCEEDED'
                """, Integer.class));
    }
}
