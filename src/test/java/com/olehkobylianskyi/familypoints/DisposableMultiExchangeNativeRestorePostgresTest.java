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
class DisposableMultiExchangeNativeRestorePostgresTest {
    @Container static final PostgreSQLContainer SOURCE=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("multi_exchange_source").withUsername("source").withPassword("qa_source");
    @Container static final PostgreSQLContainer TARGET=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("multi_exchange_target").withUsername("target").withPassword("qa_target");

    record Family(long family,long member,long points,long tokens,long rate) {}
    static Family parents(JdbcTemplate db,String label) {
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",Long.class,label);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Test member',true,'PARENT') RETURNING id
                """,Long.class,family,role);
        long points=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        long tokens=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'TOKENS','Tokens',true,1) RETURNING id
                """,Long.class,family);
        long rate=db.queryForObject("""
                INSERT INTO point_exchange_rates
                  (family_id,from_point_type_id,to_point_type_id,
                   from_amount,to_amount,approval_mode,active)
                VALUES (?,?,?,10,2,'AUTO',true) RETURNING id
                """,Long.class,family,points,tokens);
        return new Family(family,member,points,tokens,rate);
    }
    static long exchange(JdbcTemplate db,Family f,int debit,int credit) {
        long request=db.queryForObject("""
                INSERT INTO point_exchange_requests(
                  member_id,exchange_rate_id,from_point_type_id,to_point_type_id,
                  rate_from_amount,rate_to_amount,from_amount,to_amount,status,requested_at)
                VALUES (?,?,?,?,10,2,?,?,'APPROVED',NOW()) RETURNING id
                """,Long.class,f.member,f.rate,f.points,f.tokens,debit,credit);
        db.update("""
                INSERT INTO point_transactions(
                  member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,?,NOW(),'SPEND','POINT_EXCHANGE',?)
                """,f.member,f.points,-debit,request);
        db.update("""
                INSERT INTO point_transactions(
                  member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,?,NOW(),'EARN','POINT_EXCHANGE',?)
                """,f.member,f.tokens,credit,request);
        return request;
    }
    @Test void twoRequestsFourLedgerRowsOneTransactionAndLateErrorRollsBackEverything()
            throws Exception {
        var srcDs=new DriverManagerDataSource(
                SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var dstDs=new DriverManagerDataSource(
                TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(srcDs).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(dstDs).locations("classpath:db/migration").load().migrate();
        var a=new JdbcTemplate(srcDs);var b=new JdbcTemplate(dstDs);
        Family src=parents(a,"Source");
        long originalOne=exchange(a,src,10,2);
        long originalTwo=exchange(a,src,20,4);
        Family neighbor=parents(b,"Neighbor");
        long neighborRequest=exchange(b,neighbor,10,2);
        Family dst=parents(b,"Target");

        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(a,
                new WorkspaceResetSnapshotService(a,new WorkspaceEpochTransactionGuard(a)),
                codec,portable);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        byte[] archive=new TransactionTemplate(new DataSourceTransactionManager(srcDs))
                .execute(s->exporter.export(src.family,UUID.randomUUID(),UUID.randomUUID(),
                        0L,List.of("POINT_EXCHANGE_REQUESTS","POINT_EXCHANGE_RATES",
                                "POINT_TYPES","POINT_LEDGER"),"V50",key)).encryptedFile();
        var replay=new DisposableMultiExchangeNativeRestoreExecutor(b,
                new DataSourceTransactionManager(dstDs),inspector,codec,portable);
        var members=Map.of(src.member,dst.member);
        var currencies=Map.of(src.points,dst.points,src.tokens,dst.tokens);
        var rates=Map.of(src.rate,dst.rate);

        var nullMembers=new HashMap<Long,Long>();nullMembers.put(src.member,null);
        assertThrows(SecurityException.class,()->replay.apply(
                archive,src.family,dst.family,nullMembers,currencies,rates,key));
        assertThrows(SecurityException.class,()->replay.apply(
                archive,src.family,dst.family,Map.of(src.member,neighbor.member),
                currencies,rates,key));
        assertThrows(SecurityException.class,()->replay.apply(
                archive,src.family,dst.family,members,
                Map.of(src.points,neighbor.points,src.tokens,dst.tokens),rates,key));
        assertThrows(SecurityException.class,()->replay.apply(
                archive,src.family,dst.family,members,currencies,
                Map.of(src.rate,neighbor.rate),key));
        assertEquals(0L,b.queryForObject(
                "SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,dst.member));

        var tampered=archive.clone();tampered[tampered.length-1]^=1;
        assertThrows(RuntimeException.class,()->replay.apply(
                tampered,src.family,dst.family,members,currencies,rates,key));

        // The first exchange and second SPEND are successfully inserted before
        // this constraint rejects only the SECOND exchange's +4 EARN.
        b.execute("ALTER TABLE point_transactions ADD CONSTRAINT qa_reject_late_credit "+
                "CHECK (NOT (member_id="+dst.member+" AND type='EARN' AND amount=4)) NOT VALID");
        assertThrows(RuntimeException.class,()->replay.apply(
                archive,src.family,dst.family,members,currencies,rates,key));
        assertEquals(0L,b.queryForObject("""
                SELECT count(*) FROM point_exchange_requests WHERE member_id=?
                """,Long.class,dst.member));
        assertEquals(0L,b.queryForObject("""
                SELECT count(*) FROM point_transactions WHERE member_id=?
                """,Long.class,dst.member));
        b.execute("ALTER TABLE point_transactions DROP CONSTRAINT qa_reject_late_credit");

        var result=replay.apply(archive,src.family,dst.family,members,currencies,rates,key);
        assertEquals(2,result.requests().size());
        assertEquals(4,result.transactions().size());
        assertEquals(-30L,result.balances().get(dst.member+":"+dst.points));
        assertEquals(6L,result.balances().get(dst.member+":"+dst.tokens));
        var integrity=new DisposableExchangeHistoryIntegrityVerifier(
                b,inspector,portable,codec);
        var checked=integrity.verify(archive,src.family,dst.family,
                members,currencies,rates,result.requests(),result.transactions(),key);
        assertEquals(2,checked.requests());
        assertEquals(4,checked.transactions());
        assertEquals(checked.sourceSha256(),checked.targetSha256());
        assertEquals(-30L,checked.balanceDeltas().get(dst.member+":"+dst.points));
        assertEquals(6L,checked.balanceDeltas().get(dst.member+":"+dst.tokens));
        assertTrue(checked.sourceSha256().matches("[0-9a-f]{64}"));

        // Semantic changes that preserve the overall sum MUST be caught.
        Long credited= b.queryForObject("""
                SELECT id FROM point_transactions
                WHERE source_type='POINT_EXCHANGE' AND source_id=?
                  AND type='EARN'
                """,Long.class,result.requests().get(originalTwo));
        b.update("UPDATE point_transactions SET description='silent edit' WHERE id=?",credited);
        assertThrows(SecurityException.class,()->integrity.verify(
                archive,src.family,dst.family,members,currencies,rates,
                result.requests(),result.transactions(),key));
        b.update("UPDATE point_transactions SET description=NULL WHERE id=?",credited);
        b.update("""
                UPDATE point_exchange_requests
                SET requested_at=requested_at+INTERVAL '1 second' WHERE id=?
                """,result.requests().get(originalOne));
        assertThrows(SecurityException.class,()->integrity.verify(
                archive,src.family,dst.family,members,currencies,rates,
                result.requests(),result.transactions(),key));
        b.update("""
                UPDATE point_exchange_requests
                SET requested_at=requested_at-INTERVAL '1 second' WHERE id=?
                """,result.requests().get(originalOne));
        assertEquals(checked.sourceSha256(),integrity.verify(
                archive,src.family,dst.family,members,currencies,rates,
                result.requests(),result.transactions(),key).targetSha256());
        var badMaps=new HashMap<>(result.transactions());
        badMaps.put(badMaps.keySet().iterator().next(),-1L);
        assertThrows(SecurityException.class,()->integrity.verify(
                archive,src.family,dst.family,members,currencies,rates,
                result.requests(),badMaps,key));

        assertTrue(result.requests().containsKey(originalOne));
        assertTrue(result.requests().containsKey(originalTwo));
        for(Long targetRequest:result.requests().values())
            assertEquals(2L,b.queryForObject("""
                    SELECT count(*) FROM point_transactions
                    WHERE source_type='POINT_EXCHANGE' AND source_id=?
                    """,Long.class,targetRequest));
        assertEquals(2L,b.queryForObject("""
                SELECT count(*) FROM point_transactions
                WHERE source_type='POINT_EXCHANGE' AND source_id=?
                """,Long.class,neighborRequest));
        assertEquals(4L,a.queryForObject(
                "SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,src.member));
        assertThrows(IllegalStateException.class,()->replay.apply(
                archive,src.family,dst.family,members,currencies,rates,key));
        assertEquals(4L,b.queryForObject(
                "SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,dst.member));
    }
}
