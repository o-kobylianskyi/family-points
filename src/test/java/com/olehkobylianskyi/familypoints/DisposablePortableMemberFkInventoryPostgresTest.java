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
 * TEST ONLY: introspect actual migrated PG18 FK ancestry. Completeness report
 * never silently treats a new table as a reviewed portable category.
 */
@Testcontainers
class DisposablePortableMemberFkInventoryPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("member_fk_inventory").withUsername("fk_inventory")
            .withPassword("fk_inventory");

    @Test void inventoryEveryPhysicalMemberReferenceIncludingExcludedAuthAndUnreviewedSources() {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        var rows=db.queryForList("""
                SELECT t.relname AS child_table, a.attname AS child_column
                FROM pg_constraint c
                JOIN pg_class t ON t.oid=c.conrelid
                JOIN pg_namespace n ON n.oid=t.relnamespace
                CROSS JOIN LATERAL unnest(c.conkey) AS column_number
                JOIN pg_attribute a ON a.attrelid=t.oid
                                   AND a.attnum=column_number
                WHERE c.contype='f'
                  AND c.confrelid='public.family_members'::regclass
                  AND n.nspname='public'
                ORDER BY t.relname,a.attname
                """);
        var found=new HashSet<String>();
        for(var row:rows)found.add(row.get("child_table")+"."+row.get("child_column"));
        // These were verified in original migrations and should remain visible
        // even as new member-parent FK relationships are added in later versions.
        assertTrue(found.contains("task_instances.member_id"));
        assertTrue(found.contains("task_instances.original_assigned_member_id"));
        assertTrue(found.contains("task_definitions.created_by_member_id"));
        assertTrue(found.contains("point_transactions.member_id"));
        assertTrue(found.contains("reputation_transactions.member_id"));
        assertTrue(found.contains("reward_purchases.member_id"));
        assertTrue(found.contains("member_exception_periods.member_id"));

        var tableToSource=Map.ofEntries(
                Map.entry("task_instances","TASK_EXECUTIONS"),
                Map.entry("task_definitions","TASK_DEFINITIONS"),
                Map.entry("point_transactions","POINT_LEDGER"),
                Map.entry("reputation_transactions","REPUTATION_LEDGER"),
                Map.entry("reward_purchases","REWARD_PURCHASES"),
                Map.entry("point_exchange_requests","POINT_EXCHANGE_REQUESTS"),
                Map.entry("group_memberships","GROUP_MEMBERSHIPS"),
                Map.entry("task_delegations","TASK_DELEGATIONS"),
                Map.entry("member_exception_periods","MEMBER_EXCEPTION_PERIODS"));
        var reviewed=DisposablePortableMemberReferenceClosure.coveredEdges();
        var omitted=new TreeSet<String>();
        for(String fk:found) {
            int separator=fk.indexOf('.');
            String table=fk.substring(0,separator);
            String column=fk.substring(separator+1);
            String category=tableToSource.get(table);
            if(category==null || reviewed.getOrDefault(category,List.of()).stream()
                    .noneMatch(field->field.column().equals(column)))
                omitted.add(fk);
        }
        // These global-identity/account bindings are expressly outside the
        // source portable member catalog. A future reviewer must approve each
        // additional uncovered graph edge rather than treating it as complete.
        assertTrue(omitted.stream().allMatch(x->
                x.startsWith("user_accounts.")
                || x.startsWith("account_workspace_memberships.")
                || x.startsWith("workspace_")),
                "Unreviewed member FK edges require a new B-A-04 contract decision: "+omitted);
        assertFalse(found.isEmpty());
    }
}
