package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import com.olehkobylianskyi.familypoints.service.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.KeyGenerator;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DisposableExchangeLedgerSourceAuditorPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("exchange_audit").withUsername("audit").withPassword("audit_test_only");

    @Test void completedExchangeMustHaveExactlyOneDebitAndOneCreditWithoutForeignFamily()
            throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Exchange source') RETURNING id",Long.class);
        long other=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Unrelated') RETURNING id",Long.class);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Member',true,'PARENT') RETURNING id
                """,Long.class,family,role);
        long from=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        long to=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'TOKENS','Tokens',true,1) RETURNING id
                """,Long.class,family);
        long rate=db.queryForObject("""
                INSERT INTO point_exchange_rates
                    (family_id,from_point_type_id,to_point_type_id,
                     from_amount,to_amount,approval_mode,active)
                VALUES (?,?,?,10,2,'AUTO',true) RETURNING id
                """,Long.class,family,from,to);
        long request=db.queryForObject("""
                INSERT INTO point_exchange_requests
                    (member_id,exchange_rate_id,from_point_type_id,to_point_type_id,
                     rate_from_amount,rate_to_amount,from_amount,to_amount,status,requested_at)
                VALUES (?,?,?,?,10,2,10,2,'APPROVED',NOW()) RETURNING id
                """,Long.class,member,rate,from,to);
        long debitId=db.queryForObject("""
                INSERT INTO point_transactions
                    (member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,-10,NOW(),'SPEND','POINT_EXCHANGE',?) RETURNING id
                """,Long.class,member,from,request);
        long creditId=db.queryForObject("""
                INSERT INTO point_transactions
                    (member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,2,NOW(),'EARN','POINT_EXCHANGE',?) RETURNING id
                """,Long.class,member,to,request);
        var auditor=new DisposableExchangeLedgerSourceAuditor(db);
        var pair=auditor.audit(family,request);
        assertEquals(member,pair.memberId());
        assertEquals(10,pair.debited());
        assertEquals(2,pair.credited());
        assertThrows(SecurityException.class,()->auditor.audit(other,request));
        // Source request snapshot, not mutable current rate, governs history.
        db.update("UPDATE point_exchange_requests SET rate_to_amount=3 WHERE id=?",request);
        assertThrows(SecurityException.class,()->auditor.audit(family,request));
        db.update("UPDATE point_exchange_requests SET rate_to_amount=2 WHERE id=?",request);
        db.update("UPDATE point_exchange_requests SET rate_from_amount=6 WHERE id=?",request);
        assertThrows(SecurityException.class,()->auditor.audit(family,request));
        db.update("UPDATE point_exchange_requests SET rate_from_amount=10 WHERE id=?",request);
        db.update("UPDATE point_exchange_rates SET to_amount=5 WHERE id=?",rate);
        // Current mutable rate does not rewrite the request's original snapshot.
        assertEquals(2,auditor.audit(family,request).credited());
        db.update("UPDATE point_exchange_rates SET to_amount=2 WHERE id=?",rate);
        // A ledger containing POINT_EXCHANGE is never exportable without
        // the complete source request, original rate and currency catalog.
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var exporter=new WorkspacePortableBackupExporter(db,
                new WorkspaceResetSnapshotService(db,new WorkspaceEpochTransactionGuard(db)),
                codec,portable);
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var incomplete=assertThrows(IllegalStateException.class,
                ()->tx.execute(status->exporter.export(family,UUID.randomUUID(),
                        UUID.randomUUID(),0L,List.of("POINT_LEDGER"),"V50",key)));
        assertEquals("BACKUP_EXCHANGE_LEDGER_GRAPH_INCOMPLETE",incomplete.getMessage());
        var complete=tx.execute(status->exporter.export(family,UUID.randomUUID(),
                UUID.randomUUID(),0L,List.of("POINT_LEDGER","POINT_TYPES",
                        "POINT_EXCHANGE_REQUESTS","POINT_EXCHANGE_RATES"),"V50",key));
        var inspected=inspector.inspect(complete.encryptedFile(),family,key);
        assertEquals(2,inspected.counts().get("POINT_LEDGER").intValue());
        assertEquals(2,inspected.counts().get("POINT_TYPES").intValue());
        assertEquals(1,inspected.counts().get("POINT_EXCHANGE_REQUESTS").intValue());
        assertEquals(1,inspected.counts().get("POINT_EXCHANGE_RATES").intValue());
        assertFalse(inspected.blockers().contains("INVALID_EXCHANGE_LEDGER_PAIR"));
        assertFalse(inspected.blockers().contains("EXCHANGE_LEDGER_ECONOMICS_MISMATCH"));
        assertFalse(inspected.blockers().contains("MISSING_CATEGORY:POINT_EXCHANGE_RATES"));
        assertFalse(inspected.restorable());
        var copy=complete.encryptedFile().clone();copy[copy.length-1]^=1;
        assertThrows(RuntimeException.class,()->inspector.inspect(copy,family,key));
        db.update("UPDATE point_transactions SET amount=3 WHERE id=?",creditId);
        assertThrows(SecurityException.class,()->auditor.audit(family,request));
        db.update("UPDATE point_transactions SET amount=2 WHERE id=?",creditId);
        db.update("DELETE FROM point_transactions WHERE id=?",creditId);
        assertThrows(SecurityException.class,()->auditor.audit(family,request));
        db.update("""
                INSERT INTO point_transactions
                    (member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,2,NOW(),'EARN','POINT_EXCHANGE',?)
                """,member,to,request);
        db.update("UPDATE point_transactions SET amount=-9 WHERE id=?",debitId);
        assertThrows(SecurityException.class,()->auditor.audit(family,request));
        db.update("UPDATE point_transactions SET amount=-10 WHERE id=?",debitId);
        assertEquals(2,db.queryForObject("""
                SELECT count(*) FROM point_transactions
                WHERE source_type='POINT_EXCHANGE' AND source_id=?
                """,Integer.class,request));
    }
}
