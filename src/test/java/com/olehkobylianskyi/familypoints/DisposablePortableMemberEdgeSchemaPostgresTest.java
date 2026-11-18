package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TEST ONLY: compile-time schema drift guard for the proposed source-member
 * edge matrix; does not imply vNext wire approval or complete FK closure.
 */
@Testcontainers
class DisposablePortableMemberEdgeSchemaPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("source_edge_schema")
            .withUsername("source_edges").withPassword("source_edges");

    @Test void sourceMemberEdgeColumnsActuallyExistAndMatchNullability() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var tables=Map.ofEntries(
                Map.entry("TASK_EXECUTIONS","task_instances"),
                Map.entry("TASK_DEFINITIONS","task_definitions"),
                Map.entry("POINT_LEDGER","point_transactions"),
                Map.entry("REPUTATION_LEDGER","reputation_transactions"),
                Map.entry("REWARD_PURCHASES","reward_purchases"),
                Map.entry("POINT_EXCHANGE_REQUESTS","point_exchange_requests"),
                Map.entry("GROUP_MEMBERSHIPS","group_memberships"),
                Map.entry("TASK_DELEGATIONS","task_delegations"));
        for(var entry:DisposablePortableMemberReferenceClosure.coveredEdges().entrySet()) {
            String table=tables.get(entry.getKey());
            assertNotNull(table,"Unmapped source category "+entry.getKey());
            for(var field:entry.getValue()) {
                var rows=db.queryForList("""
                        SELECT is_nullable FROM information_schema.columns
                        WHERE table_schema='public'
                          AND table_name=? AND column_name=?
                        """,table,field.column());
                assertEquals(1,rows.size(),table+"."+field.column()+" must exist in PostgreSQL");
                assertEquals(field.nullable()?"YES":"NO",rows.get(0).get("is_nullable"),
                        "Source FK nullability drift "+table+"."+field.column());
            }
        }
    }
}
