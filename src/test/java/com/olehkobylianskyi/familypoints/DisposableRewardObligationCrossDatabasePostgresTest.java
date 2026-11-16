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
class DisposableRewardObligationCrossDatabasePostgresTest {
    @Container static final PostgreSQLContainer A=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("obligation_source").withUsername("source").withPassword("isolated_a");
    @Container static final PostgreSQLContainer B=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("obligation_target").withUsername("target").withPassword("isolated_b");

    private record Fixture(long family,long member,long reward,long category,
                           long purchase,long obligation) {}
    private static Fixture make(JdbcTemplate db,String familyName) {
        long family=db.queryForObject(
                "INSERT INTO families(name) VALUES (?) RETURNING id",Long.class,familyName);
        long role=db.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,family);
        long member=db.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Synthetic tester',true,'PARENT') RETURNING id
                """,Long.class,family,role);
        long currency=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        long reward=db.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Screen time',?,5) RETURNING id
                """,Long.class,family,currency);
        long category=db.queryForObject("""
                INSERT INTO reward_categories(family_id,name,active,sort_order)
                VALUES (?,'Screen',true,0) RETURNING id
                """,Long.class,family);
        long purchase=db.queryForObject("""
                INSERT INTO reward_purchases
                  (reward_definition_id,member_id,reward_title,point_type_id,price_amount,status)
                VALUES (?,?,'Screen time',?,5,'PURCHASED') RETURNING id
                """,Long.class,reward,member,currency);
        return new Fixture(family,member,reward,category,purchase,-1);
    }

    @Test void encryptedNativeObligationAndBlockReplayPreservesNeighborsAndRejectsForeignMaps()
            throws Exception {
        var sa=new DriverManagerDataSource(A.getJdbcUrl(),A.getUsername(),A.getPassword());
        var sb=new DriverManagerDataSource(B.getJdbcUrl(),B.getUsername(),B.getPassword());
        Flyway.configure().dataSource(sa).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(sb).locations("classpath:db/migration").load().migrate();
        var source=new JdbcTemplate(sa);var target=new JdbcTemplate(sb);
        Fixture src=make(source,"Source");
        long obligation=source.queryForObject("""
                INSERT INTO reward_obligations(family_id,member_id,reward_purchase_id,
                  title,status,blocking_mode)
                VALUES (?,?,?,'Complete chores','PENDING','NONE') RETURNING id
                """,Long.class,src.family(),src.member(),src.purchase());
        source.update("""
                INSERT INTO reward_obligation_blocked_categories(obligation_id,category_id)
                VALUES (?,?)
                """,obligation,src.category());
        source.update("""
                INSERT INTO reward_obligation_blocked_rewards(obligation_id,reward_definition_id)
                VALUES (?,?)
                """,obligation,src.reward());

        Fixture neighbor=make(target,"Neighbor");
        long neighborObligation=target.queryForObject("""
                INSERT INTO reward_obligations(family_id,member_id,reward_purchase_id,title,status)
                VALUES (?,?,?,'Preserved neighbor','PENDING') RETURNING id
                """,Long.class,neighbor.family(),neighbor.member(),neighbor.purchase());
        Fixture dst=make(target,"Fresh receiver");

        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(source,
                new WorkspaceResetSnapshotService(source,new WorkspaceEpochTransactionGuard(source)),
                codec,portable);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        byte[] archive=new TransactionTemplate(new DataSourceTransactionManager(sa))
                .execute(status->exporter.export(src.family(),UUID.randomUUID(),UUID.randomUUID(),
                        0L,List.of("REWARD_DEFINITIONS","REWARD_PURCHASES","REWARD_CATEGORIES",
                                "REWARD_OBLIGATIONS","REWARD_OBLIGATION_BLOCKED_CATEGORIES",
                                "REWARD_OBLIGATION_BLOCKED_REWARDS"),"V50",key)).encryptedFile();
        var replay=new DisposableRewardObligationRestoreExecutor(target,
                new DataSourceTransactionManager(sb),codec,portable,inspector);
        var members=Map.of(src.member(),dst.member());
        var purchases=Map.of(src.purchase(),dst.purchase());
        var categories=Map.of(src.category(),dst.category());
        var rewards=Map.of(src.reward(),dst.reward());
        var wrongMembers=Map.of(src.member(),neighbor.member());
        assertThrows(SecurityException.class,()->replay.apply(
                archive,src.family(),dst.family(),wrongMembers,purchases,
                Map.of(),categories,rewards,key));
        assertEquals(0L,target.queryForObject(
                "SELECT count(*) FROM reward_obligations WHERE family_id=?",
                Long.class,dst.family()));

        var result=replay.apply(archive,src.family(),dst.family(),members,purchases,
                Map.of(),categories,rewards,key);
        assertEquals(1,result.obligationIds().size());
        assertEquals(1,result.blockedCategories());
        assertEquals(1,result.blockedRewards());
        long mapped=result.obligationIds().get(obligation);
        assertEquals(1L,target.queryForObject("""
                SELECT count(*) FROM reward_obligations WHERE id=? AND family_id=?
                    AND member_id=? AND reward_purchase_id=?
                    AND title='Complete chores' AND status='PENDING'
                """,Long.class,mapped,dst.family(),dst.member(),dst.purchase()));
        assertEquals(1L,target.queryForObject("""
                SELECT count(*) FROM reward_obligation_blocked_categories
                WHERE obligation_id=? AND category_id=?
                """,Long.class,mapped,dst.category()));
        assertEquals(1L,target.queryForObject("""
                SELECT count(*) FROM reward_obligation_blocked_rewards
                WHERE obligation_id=? AND reward_definition_id=?
                """,Long.class,mapped,dst.reward()));
        assertEquals(1L,target.queryForObject("""
                SELECT count(*) FROM reward_obligations WHERE id=? AND family_id=?
                """,Long.class,neighborObligation,neighbor.family()));
        assertEquals(1L,source.queryForObject("""
                SELECT count(*) FROM reward_obligations WHERE id=? AND family_id=?
                """,Long.class,obligation,src.family()));

        assertThrows(IllegalStateException.class,()->replay.apply(
                archive,src.family(),dst.family(),members,purchases,
                Map.of(),categories,rewards,key));
        var tampered=archive.clone();tampered[tampered.length-1]^=1;
        assertThrows(RuntimeException.class,()->replay.apply(
                tampered,src.family(),dst.family(),members,purchases,
                Map.of(),categories,rewards,key));
        assertEquals(1L,target.queryForObject(
                "SELECT count(*) FROM reward_obligations WHERE family_id=?",
                Long.class,dst.family()));
    }
}
