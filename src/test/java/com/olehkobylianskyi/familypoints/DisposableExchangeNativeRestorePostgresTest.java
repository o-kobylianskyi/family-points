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
class DisposableExchangeNativeRestorePostgresTest {
    @Container static final PostgreSQLContainer SOURCE=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("native_exchange_source").withUsername("source").withPassword("test_a");
    @Container static final PostgreSQLContainer TARGET=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("native_exchange_target").withUsername("target").withPassword("test_b");

    record Family(long family,long member,long from,long to,long rate) {}
    private static Family parents(JdbcTemplate db,String name) {
        long family=db.queryForObject("INSERT INTO families(name) VALUES (?) RETURNING id",
                Long.class,name);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Synthetic parent',true,'PARENT') RETURNING id
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
        return new Family(family,member,from,to,rate);
    }
    private static long exchange(JdbcTemplate db,Family f) {
        long request=db.queryForObject("""
                INSERT INTO point_exchange_requests
                    (member_id,exchange_rate_id,from_point_type_id,to_point_type_id,
                     rate_from_amount,rate_to_amount,from_amount,to_amount,status,requested_at)
                VALUES (?,?,?,?,10,2,10,2,'APPROVED',NOW()) RETURNING id
                """,Long.class,f.member,f.rate,f.from,f.to);
        db.update("""
                INSERT INTO point_transactions
                    (member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,-10,NOW(),'SPEND','POINT_EXCHANGE',?)
                """,f.member,f.from,request);
        db.update("""
                INSERT INTO point_transactions
                    (member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,2,NOW(),'EARN','POINT_EXCHANGE',?)
                """,f.member,f.to,request);
        return request;
    }

    @Test void encryptedNativeReplayIsAtomicMapsIDsAndPreservesExistingNeighbor() throws Exception {
        var da=new DriverManagerDataSource(SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var db=new DriverManagerDataSource(TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(da).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(db).locations("classpath:db/migration").load().migrate();
        var a=new JdbcTemplate(da);var b=new JdbcTemplate(db);
        var source=parents(a,"Original");
        long sourceRequest=exchange(a,source);
        var neighbor=parents(b,"Neighbor");
        long neighborRequest=exchange(b,neighbor);
        var target=parents(b,"Restored");
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(a,
                new WorkspaceResetSnapshotService(a,new WorkspaceEpochTransactionGuard(a)),
                codec,portable);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        byte[] encrypted=new TransactionTemplate(new DataSourceTransactionManager(da))
                .execute(status->exporter.export(source.family,UUID.randomUUID(),
                        UUID.randomUUID(),0L,List.of("POINT_TYPES","POINT_LEDGER",
                            "POINT_EXCHANGE_RATES","POINT_EXCHANGE_REQUESTS"),
                        "V50",key)).encryptedFile();
        var executor=new DisposableExchangeNativeRestoreExecutor(b,
                new DataSourceTransactionManager(db),inspector,codec,portable);
        var memberMap=Map.of(source.member,target.member);
        var currencyMap=Map.of(source.from,target.from,source.to,target.to);
        var rateMap=Map.of(source.rate,target.rate);

        assertThrows(SecurityException.class,()->executor.apply(
                encrypted,source.family,target.family,
                Map.of(source.member,neighbor.member),currencyMap,rateMap,key));
        assertThrows(SecurityException.class,()->executor.apply(
                encrypted,source.family,target.family,memberMap,
                Map.of(source.from,neighbor.from,source.to,target.to),rateMap,key));
        assertThrows(SecurityException.class,()->executor.apply(
                encrypted,source.family,target.family,memberMap,currencyMap,
                Map.of(source.rate,neighbor.rate),key));
        var nullMembers=new HashMap<Long,Long>();
        nullMembers.put(source.member,null);
        assertThrows(SecurityException.class,()->executor.apply(
                encrypted,source.family,target.family,nullMembers,currencyMap,rateMap,key));
        assertEquals(0L,b.queryForObject("""
                SELECT count(*) FROM point_exchange_requests WHERE member_id=?
                """,Long.class,target.member));

        byte[] tampered=encrypted.clone();tampered[tampered.length-1]^=1;
        assertThrows(RuntimeException.class,()->executor.apply(
                tampered,source.family,target.family,memberMap,currencyMap,rateMap,key));

        // PostgreSQL NOT VALID constraint applies to NEW rows only. It forces
        // the second leg to fail after request + first SPEND have been inserted.
        b.execute("ALTER TABLE point_transactions ADD CONSTRAINT synthetic_fail_second_leg "+
                "CHECK (NOT (member_id="+target.member+" AND type='EARN')) NOT VALID");
        assertThrows(RuntimeException.class,()->executor.apply(
                encrypted,source.family,target.family,memberMap,currencyMap,rateMap,key));
        assertEquals(0L,b.queryForObject("""
                SELECT count(*) FROM point_exchange_requests WHERE member_id=?
                """,Long.class,target.member));
        assertEquals(0L,b.queryForObject("""
                SELECT count(*) FROM point_transactions WHERE member_id=?
                """,Long.class,target.member));
        b.execute("ALTER TABLE point_transactions DROP CONSTRAINT synthetic_fail_second_leg");

        var result=executor.apply(encrypted,source.family,target.family,
                memberMap,currencyMap,rateMap,key);
        assertEquals(2,result.transactionIds().size());
        assertEquals(-10L,result.balanceDeltas().get(target.from));
        assertEquals(2L,result.balanceDeltas().get(target.to));
        assertEquals(2L,b.queryForObject("""
                SELECT count(*) FROM point_transactions
                WHERE member_id=? AND source_type='POINT_EXCHANGE' AND source_id=?
                """,Long.class,target.member,result.requestId()));
        assertEquals(2L,b.queryForObject("""
                SELECT count(*) FROM point_transactions
                WHERE member_id=? AND source_type='POINT_EXCHANGE' AND source_id=?
                """,Long.class,neighbor.member,neighborRequest));
        assertEquals(2L,a.queryForObject("""
                SELECT count(*) FROM point_transactions
                WHERE member_id=? AND source_type='POINT_EXCHANGE' AND source_id=?
                """,Long.class,source.member,sourceRequest));
        assertThrows(IllegalStateException.class,()->executor.apply(
                encrypted,source.family,target.family,memberMap,currencyMap,rateMap,key));
        assertEquals(2L,b.queryForObject(
                "SELECT count(*) FROM point_transactions WHERE member_id=?",
                Long.class,target.member));
    }
}
