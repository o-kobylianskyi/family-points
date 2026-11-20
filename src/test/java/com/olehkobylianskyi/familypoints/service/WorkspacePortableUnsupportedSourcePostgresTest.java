package com.olehkobylianskyi.familypoints.service;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.spec.SecretKeySpec;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A genuinely populated unsupported category in an actual source PG18 must
 * block creation of a "complete" signed eight-category envelope. No target
 * mutation/receipt/permit is authorized or performed.
 */
class WorkspacePortableUnsupportedSourcePostgresTest {
    @Test void populatedReputationLedgerBlocksSealingBeforeAnyRestoreSideEffect() {
        try(var pg=new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("unsupported_history")
                .withUsername("qa").withPassword("qa")) {
            pg.start();
            var ds=new DriverManagerDataSource(pg.getJdbcUrl(),pg.getUsername(),pg.getPassword());
            Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            var db=new JdbcTemplate(ds);
            var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            Long family=db.queryForObject(
                    "INSERT INTO families(name) VALUES ('unsupported source') RETURNING id",Long.class);
            Long target=db.queryForObject(
                    "INSERT INTO families(name) VALUES ('untouched target') RETURNING id",Long.class);
            Long role=db.queryForObject("""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,Long.class,family);
            Long member=db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','QA only',true) RETURNING id
                    """,Long.class,family,role);
            // V12 nonempty row is outside sealed eight-category historical graph.
            db.update("""
                    INSERT INTO reputation_transactions(member_id,amount,source_type,description)
                    VALUES (?,3,'MANUAL','unsupported fixture')
                    """,member);
            assertEquals(1L,db.queryForObject("""
                    SELECT count(*) FROM reputation_transactions t
                    JOIN family_members m ON m.id=t.member_id
                    WHERE m.family_id=?
                    """,Long.class,family));
            var exporter=new WorkspacePortableAncestryV2Exporter(db,
                    new ProtectiveBackupArtifactCodec(),new ProtectiveBackupPortableFile());
            SecurityException denied=assertThrows(SecurityException.class,()->tx.execute(status->
                    exporter.exportWithHistory(family,UUID.randomUUID(),UUID.randomUUID(),0,
                            new SecretKeySpec(new byte[32],"AES"))));
            assertTrue(denied.getMessage().contains("REPUTATION") ||
                            denied.getMessage().contains("reputation_transactions"),
                    "The nonempty unsupported dependency must be the rejection reason");
            assertEquals(0L,db.queryForObject(
                    "SELECT count(*) FROM workspace_restore_permits WHERE target_workspace_id=?",
                    Long.class,target));
            assertEquals(0L,db.queryForObject(
                    "SELECT count(*) FROM workspace_restore_receipts WHERE target_workspace_id=?",
                    Long.class,target));
            assertEquals(0L,db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,target));
            assertEquals(0L,db.queryForObject(
                    "SELECT count(*) FROM task_definitions WHERE family_id=?",Long.class,target));
            assertEquals(0L,db.queryForObject(
                    "SELECT count(*) FROM reward_definitions WHERE family_id=?",Long.class,target));
        }
    }
}
