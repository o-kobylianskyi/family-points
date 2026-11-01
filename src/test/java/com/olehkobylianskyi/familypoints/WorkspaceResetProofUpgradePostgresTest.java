package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspaceResetProofUpgradePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine");

    @Test void upgradePreservesOldAuditAndAccountsButDoesNotAuthorizeLegacyProofs() throws Exception {
        var configuration=Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword())
                .locations("classpath:db/migration");
        configuration.target(MigrationVersion.fromVersion("44")).load().migrate();
        var db=new JdbcTemplate(new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword()));
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Proof upgrade fixture') RETURNING id",Long.class);
        long role=db.queryForObject("INSERT INTO family_roles(family_id,system_role,code,name) VALUES (?,true,'PARENT','Fixture') RETURNING id",Long.class,family);
        long member=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,name,member_type,active) VALUES (?,?,'Fixture','PARENT',true) RETURNING id",Long.class,family,role);
        long account=db.queryForObject("INSERT INTO user_accounts(username,password_hash,family_member_id,enabled) VALUES ('proof-upgrade','preserved-hash',?,true) RETURNING id",Long.class,member);
        UUID operation=UUID.randomUUID(),challenge=UUID.randomUUID();
        db.update("INSERT INTO workspace_reset_operations(workspace_id,operation_id,requested_by_member_id,selected_categories_json,snapshot_digest,status,confirmed_at) VALUES (?,?,?,'[]',?,'CONFIRMED',now())",
                family,operation,member,"a".repeat(64));
        db.update("INSERT INTO workspace_reset_challenges(id,workspace_id,operation_id,account_id,verifier_hash,expires_at) VALUES (?,?,?,?,?,now()+interval '5 minutes')",
                challenge,family,operation,account,"b".repeat(64));
        var flyway=Flyway.configure().dataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword())
                .locations("classpath:db/migration").load();
        assertEquals(3,flyway.migrate().migrationsExecuted);
        var row=db.queryForMap("SELECT * FROM workspace_reset_challenges WHERE id=?",challenge);
        assertEquals("b".repeat(64),row.get("verifier_hash"));
        assertNull(row.get("consumed_at")); assertNull(row.get("data_epoch"));
        assertNull(row.get("snapshot_digest")); assertNull(row.get("requested_by_member_id")); assertNull(row.get("credential_fingerprint"));
        assertEquals("CONFIRMED",db.queryForObject("SELECT status FROM workspace_reset_operations WHERE operation_id=?",String.class,operation));
        assertEquals("preserved-hash",db.queryForObject("SELECT password_hash FROM user_accounts WHERE id=?",String.class,account));
        assertEquals(true,db.queryForObject("SELECT enabled FROM user_accounts WHERE id=?",Boolean.class,account));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,family));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->db.update("UPDATE workspace_reset_challenges SET data_epoch=0 WHERE id=?",challenge));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->db.update("UPDATE workspace_reset_challenges SET data_epoch=-1,snapshot_digest=?,requested_by_member_id=?,credential_fingerprint=? WHERE id=?",
                        "a".repeat(64),member,"c".repeat(64),challenge));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->db.update("UPDATE workspace_reset_challenges SET data_epoch=0,snapshot_digest=?,requested_by_member_id=?,credential_fingerprint=? WHERE id=?",
                        "not-a-digest",member,"c".repeat(64),challenge));
        assertEquals(0,flyway.migrate().migrationsExecuted);

        // Publish SCHEMA METADATA from a disposable database only, never tenant row contents.
        var tables=db.queryForList("""
                SELECT c.relname AS table_name,
                  coalesce(string_agg(a.attname,',' ORDER BY a.attnum)
                    FILTER(WHERE a.attname IN ('family_id','workspace_id')),'') AS scope_columns
                FROM pg_catalog.pg_class c
                JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
                JOIN pg_catalog.pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped
                WHERE n.nspname='public' AND c.relkind IN ('r','p') AND c.relname<>'flyway_schema_history'
                GROUP BY c.relname ORDER BY c.relname
                """);
        var foreignKeys=db.queryForList("""
                SELECT child.relname AS dependent,c.conname AS constraint_name,
                  string_agg(ca.attname,',' ORDER BY k.idx) AS child_columns,
                  parent.relname AS referenced,
                  string_agg(pa.attname,',' ORDER BY k.idx) AS parent_columns,
                  CASE c.confdeltype WHEN 'c' THEN 'CASCADE' WHEN 'r' THEN 'RESTRICT'
                    WHEN 'n' THEN 'SET NULL' WHEN 'd' THEN 'SET DEFAULT' ELSE 'NO ACTION' END AS delete_action
                FROM pg_catalog.pg_constraint c
                JOIN pg_catalog.pg_class child ON child.oid=c.conrelid
                JOIN pg_catalog.pg_class parent ON parent.oid=c.confrelid
                JOIN pg_catalog.pg_namespace cn ON cn.oid=child.relnamespace AND cn.nspname='public'
                JOIN pg_catalog.pg_namespace pn ON pn.oid=parent.relnamespace AND pn.nspname='public'
                CROSS JOIN LATERAL unnest(c.conkey,c.confkey) WITH ORDINALITY AS k(child_att,parent_att,idx)
                JOIN pg_catalog.pg_attribute ca ON ca.attrelid=child.oid AND ca.attnum=k.child_att
                JOIN pg_catalog.pg_attribute pa ON pa.attrelid=parent.oid AND pa.attnum=k.parent_att
                WHERE c.contype='f'
                GROUP BY child.relname,c.conname,parent.relname,c.confdeltype
                ORDER BY child.relname,c.conname
                """);
        assertFalse(tables.isEmpty()); assertFalse(foreignKeys.isEmpty());
        System.out.println("RESET_POLICY_SCHEMA="+new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(Map.of("tables",tables,"foreignKeys",foreignKeys)));
    }
}
