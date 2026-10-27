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

        // Tests share a static container; verify migration constraints and statuses,
        // not an empty table that depends on test execution order.
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

    @Test void resetAuditKeysAreTenantScopedAndCannotSilentlyOverwriteOrDelete() {
        Flyway.configure().dataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var db = new JdbcTemplate(new DriverManagerDataSource(
                DB.getJdbcUrl(), DB.getUsername(), DB.getPassword()));
        long familyA = db.queryForObject(
                "INSERT INTO families(name) VALUES ('Reset audit A') RETURNING id", Long.class);
        long familyB = db.queryForObject(
                "INSERT INTO families(name) VALUES ('Reset audit B') RETURNING id", Long.class);
        long roleA = db.queryForObject("""
                INSERT INTO family_roles(family_id,system_role,code,name)
                VALUES (?,true,'PARENT','Parent') RETURNING id
                """, Long.class, familyA);
        long roleB = db.queryForObject("""
                INSERT INTO family_roles(family_id,system_role,code,name)
                VALUES (?,true,'PARENT','Parent') RETURNING id
                """, Long.class, familyB);
        long memberA = db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,active,member_type,name)
                VALUES (?,?,true,'PARENT','Reset admin A') RETURNING id
                """, Long.class, familyA, roleA);
        long memberB = db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,active,member_type,name)
                VALUES (?,?,true,'PARENT','Reset admin B') RETURNING id
                """, Long.class, familyB, roleB);
        var operation = java.util.UUID.randomUUID().toString();
        String digest = "a".repeat(64);
        String claim = """
                INSERT INTO workspace_reset_operations
                  (workspace_id,operation_id,requested_by_member_id,selected_categories_json,snapshot_digest)
                VALUES (?,?::uuid,?,?,?)
                ON CONFLICT (workspace_id,operation_id) DO NOTHING
                """;
        assertEquals(1, db.update(claim, familyA, operation, memberA, "[\"PRIVATE_TEMPLATES\"]", digest));
        assertEquals(0, db.update(claim, familyA, operation, memberA, "[\"TASK_DEFINITIONS\"]", digest));
        assertEquals(1, db.update(claim, familyB, operation, memberB, "[]", digest));
        assertEquals(2, db.queryForObject("SELECT count(*) FROM workspace_reset_operations", Integer.class));
        assertEquals("[\"PRIVATE_TEMPLATES\"]", db.queryForObject("""
                SELECT selected_categories_json FROM workspace_reset_operations
                WHERE workspace_id=? AND operation_id=?::uuid
                """, String.class, familyA, operation));
        assertEquals(2, db.queryForObject("""
                SELECT count(*) FROM workspace_reset_operations WHERE status='PREPARED'
                """, Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> db.update("DELETE FROM families WHERE id=?", familyA));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> db.update("""
                        UPDATE workspace_reset_operations SET status='SUCCEEDED'
                        WHERE workspace_id=? AND operation_id=?::uuid
                        """, familyA, operation));
        assertEquals(2, db.queryForObject("SELECT count(*) FROM workspace_reset_operations", Integer.class));
    }
}
