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

@Testcontainers
class DisposablePointLedgerCrossDatabasePostgresTest {
    @Container static final PostgreSQLContainer A=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("finance_source").withUsername("source").withPassword("isolated_a");
    @Container static final PostgreSQLContainer B=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("finance_target").withUsername("target").withPassword("isolated_b");

    record Family(long id,long member,long points) {}
    private static Family create(JdbcTemplate db,String name) {
        long family=db.queryForObject("INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class,name);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Test parent',true,'PARENT') RETURNING id
                """,Long.class,family,role);
        long pointType=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        return new Family(family,member,pointType);
    }
    @Test void independentManualLedgerReconcilesPerMemberCurrencyAndPreservesNeighbor()
            throws Exception {
        var as=new DriverManagerDataSource(A.getJdbcUrl(),A.getUsername(),A.getPassword());
        var bs=new DriverManagerDataSource(B.getJdbcUrl(),B.getUsername(),B.getPassword());
        Flyway.configure().dataSource(as).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(bs).locations("classpath:db/migration").load().migrate();
        var a=new JdbcTemplate(as);var b=new JdbcTemplate(bs);
        var src=create(a,"Source ledger");
        var neighbor=create(b,"Neighbor ledger");
        b.update("""
                INSERT INTO point_transactions
                 (member_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,99,NOW(),'EARN','MANUAL','Untouched')
                """,neighbor.member,neighbor.points);
        var target=create(b,"Fresh replay");
        a.update("""
                INSERT INTO point_transactions
                 (member_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,12,NOW(),'EARN','MANUAL','Earned')
                """,src.member,src.points);
        a.update("""
                INSERT INTO point_transactions
                 (member_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,-2,NOW(),'SPEND','MANUAL','Spent')
                """,src.member,src.points);
        var codec=new ProtectiveBackupArtifactCodec();
        var files=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(files,codec);
        var exporter=new WorkspacePortableBackupExporter(a,
                new WorkspaceResetSnapshotService(a,new WorkspaceEpochTransactionGuard(a)),
                codec,files);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        byte[] archive=new TransactionTemplate(new DataSourceTransactionManager(as))
                .execute(s->exporter.export(src.id,UUID.randomUUID(),UUID.randomUUID(),0L,
                        List.of("POINT_LEDGER","POINT_TYPES"),"V50",key)).encryptedFile();
        var catalog=inspector.inspect(archive,src.id,key);
        assertEquals(1,catalog.counts().get("POINT_TYPES").intValue());
        assertFalse(catalog.blockers().contains("MISSING_CATEGORY:POINT_TYPES"));
        assertFalse(catalog.blockers().contains("DANGLING_REFERENCE:POINT_LEDGER.point_type_id"));
        var replay=new DisposablePointLedgerRestoreExecutor(b,
                new DataSourceTransactionManager(bs),files,codec,inspector);
        var members=Map.of(src.member,target.member);
        var currency=Map.of(src.points,target.points);
        var currencyCodes=Map.of(src.points,"POINTS");
        assertThrows(SecurityException.class,()->replay.apply(
                archive,src.id,target.id,Map.of(src.member,neighbor.member),
                currency,currencyCodes,key));
        assertThrows(SecurityException.class,()->replay.apply(
                archive,src.id,target.id,members,currency,
                Map.of(src.points,"WRONG"),key));
        assertEquals(0L,b.queryForObject("""
                SELECT count(*) FROM point_transactions WHERE member_id=?
                """,Long.class,target.member));
        byte[] legacyArchive=new TransactionTemplate(new DataSourceTransactionManager(as))
                .execute(s->exporter.export(src.id,UUID.randomUUID(),UUID.randomUUID(),0L,
                        List.of("POINT_LEDGER"),"V50",key)).encryptedFile();
        var legacyReport=inspector.inspect(legacyArchive,src.id,key);
        assertTrue(legacyReport.blockers().contains("MISSING_CATEGORY:POINT_TYPES"));
        assertThrows(IllegalArgumentException.class,()->replay.apply(
                legacyArchive,src.id,target.id,members,currency,currencyCodes,key));
        assertEquals(0L,b.queryForObject(
                "SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,target.member));
        var corrupted=archive.clone();
        corrupted[corrupted.length-1]^=1;
        assertThrows(RuntimeException.class,()->replay.apply(
                corrupted,src.id,target.id,members,currency,currencyCodes,key));
        assertEquals(0L,b.queryForObject(
                "SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,target.member));
        var out=replay.apply(archive,src.id,target.id,members,currency,currencyCodes,key);
        assertEquals(2,out.count());
        assertEquals(2,out.transactionIds().size());
        assertEquals(10L,out.balances().get(target.member+":"+target.points));
        assertEquals(10L,b.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM point_transactions
                WHERE member_id=? AND point_type_id=?
                """,Long.class,target.member,target.points));
        assertEquals(99L,b.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM point_transactions
                WHERE member_id=? AND point_type_id=?
                """,Long.class,neighbor.member,neighbor.points));
        assertEquals(10L,a.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM point_transactions
                WHERE member_id=? AND point_type_id=?
                """,Long.class,src.member,src.points));
        assertThrows(IllegalStateException.class,()->replay.apply(
                archive,src.id,target.id,members,currency,currencyCodes,key));
        assertEquals(2L,b.queryForObject("SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,target.member));

        a.update("""
                INSERT INTO point_transactions
                 (member_id,point_type_id,amount,created_at,type,source_type,source_id,description)
                VALUES (?, ?, 1, NOW(),'EARN','TASK',42,'Polymorphic')
                """,src.member,src.points);
        byte[] nonmanual=new TransactionTemplate(new DataSourceTransactionManager(as))
                .execute(s->exporter.export(src.id,UUID.randomUUID(),UUID.randomUUID(),0L,
                        List.of("POINT_LEDGER","POINT_TYPES"),"V50",key)).encryptedFile();
        var fresh=create(b,"Blocked polymorphic target");
        assertThrows(SecurityException.class,()->replay.apply(
                nonmanual,src.id,fresh.id,Map.of(src.member,fresh.member),
                Map.of(src.points,fresh.points),currencyCodes,key));
        assertEquals(0L,b.queryForObject(
                "SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,fresh.member));
    }
}
