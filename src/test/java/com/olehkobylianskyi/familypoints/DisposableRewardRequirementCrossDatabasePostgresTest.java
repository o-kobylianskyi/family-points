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
class DisposableRewardRequirementCrossDatabasePostgresTest {
    @Container static final PostgreSQLContainer SOURCE=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("requirements_a").withUsername("source").withPassword("isolation_a");
    @Container static final PostgreSQLContainer TARGET=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("requirements_b").withUsername("target").withPassword("isolation_b");

    @Test void exactRequirementsRemapIsAtomicRejectsForeignParentAndLeavesNeighborUntouched()
            throws Exception {
        var dsA=new DriverManagerDataSource(
                SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var dsB=new DriverManagerDataSource(
                TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(dsA).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(dsB).locations("classpath:db/migration").load().migrate();
        var a=new JdbcTemplate(dsA);var b=new JdbcTemplate(dsB);
        long source=a.queryForObject(
                "INSERT INTO families(name) VALUES ('Source') RETURNING id",Long.class);
        long sourcePoints=a.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,source);
        long sourceReward=a.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Read books',?,7) RETURNING id
                """,Long.class,source,sourcePoints);
        long oldRequirement=a.queryForObject("""
                INSERT INTO reward_requirements
                  (family_id,reward_definition_id,phase,requirement_type,required,sort_order)
                VALUES (?,?,'BEFORE_PURCHASE','TASK_COMPLETION',true,3) RETURNING id
                """,Long.class,source,sourceReward);

        long neighbor=b.queryForObject(
                "INSERT INTO families(name) VALUES ('Neighbor') RETURNING id",Long.class);
        long neighborPoints=b.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,neighbor);
        long neighborReward=b.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Untouched',?,2) RETURNING id
                """,Long.class,neighbor,neighborPoints);
        long neighborRequirement=b.queryForObject("""
                INSERT INTO reward_requirements
                  (family_id,reward_definition_id,phase,requirement_type,required,sort_order)
                VALUES (?,?,'BEFORE_PURCHASE','TASK_COMPLETION',true,2) RETURNING id
                """,Long.class,neighbor,neighborReward);

        long target=b.queryForObject(
                "INSERT INTO families(name) VALUES ('Restored') RETURNING id",Long.class);
        long targetPoints=b.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,target);
        long targetReward=b.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Read books',?,7) RETURNING id
                """,Long.class,target,targetPoints);
        assertNotEquals(source,target);

        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(a,
                new WorkspaceResetSnapshotService(a,new WorkspaceEpochTransactionGuard(a)),
                codec,portable);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        byte[] archive=new TransactionTemplate(new DataSourceTransactionManager(dsA))
                .execute(status->exporter.export(source,UUID.randomUUID(),UUID.randomUUID(),
                        0L,List.of("REWARD_DEFINITIONS","REWARD_REQUIREMENTS"),
                        "V50",key)).encryptedFile();
        var replay=new DisposableRewardRequirementRestoreExecutor(b,
                new DataSourceTransactionManager(dsB),inspector,codec,portable);

        var nullMapping=new HashMap<Long,Long>();
        nullMapping.put(sourceReward,null);
        assertThrows(SecurityException.class,()->replay.apply(
                archive,source,target,nullMapping,Map.of(),Map.of(),key));
        assertThrows(SecurityException.class,()->replay.apply(
                archive,source,target,Map.of(sourceReward,neighborReward),
                Map.of(),Map.of(),key));
        assertEquals(0L,b.queryForObject(
                "SELECT count(*) FROM reward_requirements WHERE family_id=?",
                Long.class,target));

        var result=replay.apply(archive,source,target,
                Map.of(sourceReward,targetReward),Map.of(),Map.of(),key);
        assertEquals(1,result.ids().size());
        long newRequirement=result.ids().get(oldRequirement);
        assertNotEquals(oldRequirement,newRequirement);
        assertEquals(1L,b.queryForObject("""
                SELECT count(*) FROM reward_requirements
                WHERE id=? AND family_id=? AND reward_definition_id=?
                    AND phase='BEFORE_PURCHASE' AND requirement_type='TASK_COMPLETION'
                    AND required=true AND sort_order=3
                """,Long.class,newRequirement,target,targetReward));
        assertEquals(1L,b.queryForObject(
                "SELECT count(*) FROM reward_requirements WHERE id=? AND family_id=?",
                Long.class,neighborRequirement,neighbor));
        assertEquals(1L,a.queryForObject(
                "SELECT count(*) FROM reward_requirements WHERE id=? AND family_id=?",
                Long.class,oldRequirement,source));
        assertThrows(IllegalStateException.class,()->replay.apply(
                archive,source,target,Map.of(sourceReward,targetReward),
                Map.of(),Map.of(),key));
        assertEquals(1L,b.queryForObject(
                "SELECT count(*) FROM reward_requirements WHERE family_id=?",
                Long.class,target));
        var tampered=archive.clone();tampered[tampered.length-1]^=1;
        assertThrows(RuntimeException.class,()->replay.apply(
                tampered,source,target,Map.of(sourceReward,targetReward),
                Map.of(),Map.of(),key));
    }
}
