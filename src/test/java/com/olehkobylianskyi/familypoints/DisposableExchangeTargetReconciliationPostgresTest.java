package com.olehkobylianskyi.familypoints;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class DisposableExchangeTargetReconciliationPostgresTest {
    @Container static final PostgreSQLContainer SOURCE=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("exchange_src").withUsername("source").withPassword("source_test");
    @Container static final PostgreSQLContainer TARGET=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("exchange_dst").withUsername("target").withPassword("target_test");

    record Fixture(long family,long member,long from,long to,long request,
                   long rate,long creditLedger) {}
    private static Fixture create(JdbcTemplate db,String label) {
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",Long.class,label);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Synthetic member',true,'PARENT') RETURNING id
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
                    (family_id,from_point_type_id,to_point_type_id,from_amount,
                     to_amount,approval_mode,active)
                VALUES (?,?,?,10,2,'AUTO',true) RETURNING id
                """,Long.class,family,from,to);
        long request=db.queryForObject("""
                INSERT INTO point_exchange_requests
                    (member_id,exchange_rate_id,from_point_type_id,to_point_type_id,
                     rate_from_amount,rate_to_amount,from_amount,to_amount,status,requested_at)
                VALUES (?,?,?,?,10,2,10,2,'APPROVED',NOW()) RETURNING id
                """,Long.class,member,rate,from,to);
        db.update("""
                INSERT INTO point_transactions
                    (member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,-10,NOW(),'SPEND','POINT_EXCHANGE',?)
                """,member,from,request);
        long creditId=db.queryForObject("""
                INSERT INTO point_transactions
                    (member_id,point_type_id,amount,created_at,type,source_type,source_id)
                VALUES (?,?,2,NOW(),'EARN','POINT_EXCHANGE',?) RETURNING id
                """,Long.class,member,to,request);
        return new Fixture(family,member,from,to,request,rate,creditId);
    }
    @Test void separateDatabasesRequireExplicitMappingsAndBothLedgerLegs()
            throws Exception {
        var srcDs=new DriverManagerDataSource(
                SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var dstDs=new DriverManagerDataSource(
                TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(srcDs).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(dstDs).locations("classpath:db/migration").load().migrate();
        var a=new JdbcTemplate(srcDs);
        var b=new JdbcTemplate(dstDs);
        var src=create(a,"Original");
        var neighbor=create(b,"Unrelated neighbor");
        var dst=create(b,"Reconstructed target");
        var verifier=new DisposableExchangeTargetReconciliationVerifier(a,b);
        var members=Map.of(src.member,dst.member);
        var currencies=Map.of(src.from,dst.from,src.to,dst.to);
        var result=verifier.verify(src.family,dst.family,src.request,dst.request,
                members,currencies);
        assertEquals(10,result.debited());
        assertEquals(2,result.credited());
        assertEquals("POINTS",result.fromCode());
        assertEquals("TOKENS",result.toCode());

        assertThrows(SecurityException.class,()->verifier.verify(
                src.family,dst.family,src.request,dst.request,
                Map.of(src.member,neighbor.member),currencies));
        assertThrows(SecurityException.class,()->verifier.verify(
                src.family,dst.family,src.request,dst.request,
                members,Map.of(src.from,neighbor.from,src.to,neighbor.to)));
        assertThrows(SecurityException.class,()->verifier.verify(
                src.family,neighbor.family,src.request,neighbor.request,members,currencies));

        b.update("UPDATE point_types SET code='COINS' WHERE id=?",dst.to);
        assertThrows(SecurityException.class,()->verifier.verify(
                src.family,dst.family,src.request,dst.request,members,currencies));
        b.update("UPDATE point_types SET code='TOKENS' WHERE id=?",dst.to);

        b.update("UPDATE point_exchange_requests SET rate_from_amount=5,rate_to_amount=1 WHERE id=?",
                dst.request);
        assertThrows(SecurityException.class,()->verifier.verify(
                src.family,dst.family,src.request,dst.request,members,currencies));
        b.update("UPDATE point_exchange_requests SET rate_from_amount=10,rate_to_amount=2 WHERE id=?",
                dst.request);

        b.update("UPDATE point_exchange_requests SET rate_to_amount=3,to_amount=3 WHERE id=?",
                dst.request);
        b.update("UPDATE point_transactions SET amount=3 WHERE id=?",dst.creditLedger);
        assertThrows(SecurityException.class,()->verifier.verify(
                src.family,dst.family,src.request,dst.request,members,currencies));
        b.update("UPDATE point_exchange_requests SET rate_to_amount=2,to_amount=2 WHERE id=?",
                dst.request);
        b.update("UPDATE point_transactions SET amount=2 WHERE id=?",dst.creditLedger);

        b.update("DELETE FROM point_transactions WHERE id=?",dst.creditLedger);
        assertThrows(SecurityException.class,()->verifier.verify(
                src.family,dst.family,src.request,dst.request,members,currencies));
        assertEquals(2L,a.queryForObject("""
                SELECT count(*) FROM point_transactions
                WHERE source_type='POINT_EXCHANGE' AND source_id=?
                """,Long.class,src.request));
        assertEquals(2L,b.queryForObject("""
                SELECT count(*) FROM point_transactions
                WHERE source_type='POINT_EXCHANGE' AND source_id=?
                """,Long.class,neighbor.request));
    }
}
