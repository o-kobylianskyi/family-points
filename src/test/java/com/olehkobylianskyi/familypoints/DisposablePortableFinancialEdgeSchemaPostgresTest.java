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

/** TEST ONLY: verify reviewed financial source columns against migrated PG18. */
@Testcontainers
class DisposablePortableFinancialEdgeSchemaPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("finance_source_schema")
            .withUsername("finance_source").withPassword("finance_source");

    @Test void everyReviewedGraphEdgeExistsWithExplicitNullability() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var mapping=Map.ofEntries(
                Map.entry("POINT_LEDGER","point_transactions"),
                Map.entry("REPUTATION_LEDGER","reputation_transactions"),
                Map.entry("REWARD_PURCHASES","reward_purchases"),
                Map.entry("GROUP_POINT_LEDGER","group_point_transactions"),
                Map.entry("GROUP_MEMBERSHIPS","group_memberships"),
                Map.entry("TASK_EXECUTIONS","task_instances"),
                Map.entry("TASK_DEFINITIONS","task_definitions"),
                Map.entry("REWARD_DEFINITIONS","reward_definitions"),
                Map.entry("POINT_EXCHANGE_REQUESTS","point_exchange_requests"),
                Map.entry("POINT_EXCHANGE_RATES","point_exchange_rates"));
        for(var entry:DisposablePortableFinancialSourceClosure.reviewedParents().entrySet()) {
            String table=mapping.get(entry.getKey());
            assertNotNull(table,entry.getKey());
            for(var parent:entry.getValue()) {
                var cols=db.queryForList("""
                    SELECT is_nullable FROM information_schema.columns
                    WHERE table_schema='public' AND table_name=? AND column_name=?
                    """,table,parent.column());
                assertEquals(1,cols.size(),"Missing source edge "+table+"."+parent.column());
                assertEquals(parent.optional()?"YES":"NO",cols.get(0).get("is_nullable"),
                        "Changed source nullability "+table+"."+parent.column());
            }
        }
        var parentTables=Map.of(
                "POINT_TYPES","point_types",
                "MEMBER_GROUPS","member_groups",
                "REWARD_DEFINITIONS","reward_definitions",
                "TASK_DEFINITIONS","task_definitions",
                "POINT_EXCHANGE_RATES","point_exchange_rates");
        for(var source:parentTables.entrySet()) {
            for(String column:List.of("id","family_id")) {
                var rows=db.queryForList("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema='public' AND table_name=? AND column_name=?
                    """,source.getValue(),column);
                assertEquals(1,rows.size(),"Source parent lacks "+source.getValue()+"."+column);
            }
        }
    }
}
