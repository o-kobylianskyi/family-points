package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Export authentic, untrusted financial history; reject incomplete source
 * identity mappings without ever altering family balances. */
@Testcontainers
class WorkspacePointLedgerSourceClosurePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("ledger_sources").withUsername("ledger_sources")
            .withPassword("isolated_sources");

    @Test void polymorphicTaskSourceIdCannotBeAssumedRestorable() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long source=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Ledger source') RETURNING id",Long.class);
        long neighbor=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Unrelated family') RETURNING id",Long.class);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,source);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Owner',true,'PARENT') RETURNING id
                """,Long.class,source,role);
        long currency=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,source);
        long neighborCurrency=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,neighbor);
        db.update("""
                INSERT INTO point_transactions
                  (member_id,point_type_id,amount,created_at,type,source_type,source_id,description)
                VALUES (?,?,12,NOW(),'EARN','TASK',4242,'Historic task grant')
                """,member,currency);
        db.update("""
                INSERT INTO point_transactions
                  (member_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,-2,NOW(),'SPEND','MANUAL','Historic manual debit')
                """,member,currency);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,
                new WorkspaceResetSnapshotService(db,new WorkspaceEpochTransactionGuard(db)),
                codec,portable);
        var keys=KeyGenerator.getInstance("AES");keys.init(256);
        var key=keys.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        byte[] file=tx.execute(status->exporter.export(source,UUID.randomUUID(),
                UUID.randomUUID(),0L,List.of("POINT_LEDGER"),"V50",key)).encryptedFile();
        var report=inspector.inspect(file,source,key);
        assertEquals(2,report.counts().get("POINT_LEDGER").intValue());
        assertFalse(report.restorable());
        assertTrue(report.blockers().contains("HISTORICAL_SOURCE_REMAP_REQUIRED:POINT_LEDGER:TASK"));
        assertTrue(report.blockers().contains("HISTORICAL_SOURCE_ID_REMAP_REQUIRED:POINT_LEDGER"));
        assertTrue(report.blockers().contains("CURRENCY_REBIND_REQUIRED:POINT_LEDGER.point_type_id"));
        assertTrue(report.blockers().contains("IDENTITY_REBIND_REQUIRED:POINT_LEDGER.member_id"));
        assertEquals(10L,db.queryForObject(
                "SELECT sum(amount) FROM point_transactions WHERE member_id=?",
                Long.class,member).longValue());
        assertThrows(SecurityException.class,()->inspector.inspect(file,neighbor,key));
        // The schema protects point_type foreign keys but not their family
        // equivalence. A deliberately cross-family denomination must be
        // rejected BEFORE an AES archive is produced.
        db.update("""
                INSERT INTO point_transactions
                  (member_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,1,NOW(),'EARN','MANUAL','Invalid cross-tenant denomination')
                """,member,neighborCurrency);
        assertThrows(SecurityException.class,()->tx.execute(status->exporter.export(
                source,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("POINT_LEDGER"),"V50",key)));
    }
}
