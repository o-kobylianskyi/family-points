package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.WorkspaceResetDependencyAuditService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspaceResetDependencyAuditPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("reset_dependency").withUsername("reset_audit")
            .withPassword("reset_audit_password");

    @Test void introspectionHasActualForeignKeysAndNeverMutatesWorkspace() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        Long family=jdbc.queryForObject(
                "INSERT INTO families(name) VALUES ('Read-only safety audit') RETURNING id",Long.class);
        assertNotNull(family);
        var service=new WorkspaceResetDependencyAuditService(jdbc);
        long before=jdbc.queryForObject("SELECT count(*) FROM workspace_setup_states WHERE workspace_id=?",
                Long.class,family);
        var report=service.audit(family);
        assertFalse(report.executionEnabled());
        assertFalse(report.dependenciesApproved());
        assertTrue(report.foreignKeys().size()>10,"Must inspect real Flyway FK graph");
        assertTrue(report.foreignKeys().stream().anyMatch(fk->fk.referencedTable().equals("families")));
        assertTrue(report.protectedTables().contains("point_transactions"));
        assertTrue(report.blockers().contains("RESET_EXECUTION_DISABLED"));
        assertEquals(before,jdbc.queryForObject(
                "SELECT count(*) FROM workspace_setup_states WHERE workspace_id=?",Long.class,family));
        assertEquals("Read-only safety audit",jdbc.queryForObject(
                "SELECT name FROM families WHERE id=?",String.class,family));
        var selection=service.inspectSelection(family,java.util.List.of(
                "TASK_DEFINITIONS","POINT_LEDGER","TASK_DEFINITIONS"));
        assertFalse(selection.executable());
        assertEquals(java.util.List.of("POINT_LEDGER","TASK_DEFINITIONS"),
                selection.selectedCategories());
        assertTrue(selection.blockers().contains("PROTECTED_LEDGER:POINT_LEDGER"));
        assertTrue(selection.blockers().contains("TASK_DEFINITIONS_REQUIRES_EXECUTIONS_AND_SLOTS"));
        assertFalse(selection.relevantForeignKeys().isEmpty());
        assertTrue(selection.blockers().stream().anyMatch(x->x.startsWith("EXTERNAL_FK_DEPENDENCY:")));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->service.inspectSelection(family,java.util.List.of("UNKNOWN"))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->service.inspectSelection(family,java.util.List.of())).getStatusCode());
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM families WHERE id=?",
                Long.class,family));
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->service.audit(Long.MAX_VALUE)).getStatusCode());
    }
}
