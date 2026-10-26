package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class TemplateActivationIdempotencyPostgresTest {
    @Container
    static final PostgreSQLContainer DB = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("template_activation_regression")
            .withUsername("template_test")
            .withPassword("template_test_password");

    @Test void duplicateKeysAreScopedAndFailedActivationRollsBackClaim() {
        Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword())
                .locations("classpath:db/migration").load().migrate();
        var source = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        var db = new JdbcTemplate(source);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        long familyA=db.queryForObject("INSERT INTO families(name) VALUES ('A') RETURNING id",Long.class);
        long familyB=db.queryForObject("INSERT INTO families(name) VALUES ('B') RETURNING id",Long.class);
        long templateA=db.queryForObject("""
                INSERT INTO workspace_private_templates(workspace_id,kind,title,definition_json)
                VALUES(?,?,?,?) RETURNING id
                """,Long.class,familyA,"TASK","Test","{}");
        long templateB=db.queryForObject("""
                INSERT INTO workspace_private_templates(workspace_id,kind,title,definition_json)
                VALUES(?,?,?,?) RETURNING id
                """,Long.class,familyB,"TASK","Test","{}");
        UUID key=UUID.randomUUID();
        String claim="""
                INSERT INTO template_activation_requests(workspace_id,operation_id,template_id,kind)
                VALUES(?,?,?,?) ON CONFLICT (workspace_id,operation_id) DO NOTHING
                """;
        assertEquals(1,db.update(claim,familyA,key,templateA,"TASK"));
        assertEquals(0,db.update(claim,familyA,key,templateA,"TASK"));
        assertEquals(1,db.update(claim,familyB,key,templateB,"TASK"));
        UUID rolledBack=UUID.randomUUID();
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{
            assertEquals(1,db.update(claim,familyA,rolledBack,templateA,"TASK"));
            throw new IllegalStateException("Simulated create failure");
        }));
        assertEquals(1,db.update(claim,familyA,rolledBack,templateA,"TASK"));
        assertEquals(3,db.queryForObject("SELECT count(*) FROM template_activation_requests",Integer.class));
    }
}
