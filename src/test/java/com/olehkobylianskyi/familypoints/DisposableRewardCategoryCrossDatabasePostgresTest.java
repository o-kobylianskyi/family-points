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

/** Two physically independent databases; only synthetic family data. */
@Testcontainers
class DisposableRewardCategoryCrossDatabasePostgresTest {
    @Container static final PostgreSQLContainer SOURCE=
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("category_source").withUsername("source")
                    .withPassword("source_test_only");
    @Container static final PostgreSQLContainer TARGET=
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("category_target").withUsername("target")
                    .withPassword("target_test_only");

    @Test void independentlyMigratedDatabasesRebindCategoryLinksAndPreserveOtherFamily()
            throws Exception {
        var sds=new DriverManagerDataSource(
                SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var tds=new DriverManagerDataSource(
                TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(sds).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(tds).locations("classpath:db/migration").load().migrate();
        var src=new JdbcTemplate(sds);
        var dst=new JdbcTemplate(tds);
        long family=src.queryForObject(
                "INSERT INTO families(name) VALUES ('Source') RETURNING id",Long.class);
        long sourceCurrency=src.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        long definition=src.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Reward',?,10) RETURNING id
                """,Long.class,family,sourceCurrency);
        long category=src.queryForObject("""
                INSERT INTO reward_categories(family_id,name,active,sort_order)
                VALUES (?,'Learning',true,2) RETURNING id
                """,Long.class,family);
        src.update("""
                INSERT INTO reward_definition_categories(reward_definition_id,category_id)
                VALUES (?,?)
                """,definition,category);

        long neighbor=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Preserved neighbor') RETURNING id",Long.class);
        long neighborCategory=dst.queryForObject("""
                INSERT INTO reward_categories(family_id,name,active,sort_order)
                VALUES (?,'Neighbor category',true,1) RETURNING id
                """,Long.class,neighbor);
        long neighborCurrency=dst.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,neighbor);
        long neighborDefinition=dst.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Neighbor reward',?,2) RETURNING id
                """,Long.class,neighbor,neighborCurrency);
        dst.update("""
                INSERT INTO reward_definition_categories(reward_definition_id,category_id)
                VALUES (?,?)
                """,neighborDefinition,neighborCategory);
        long target=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Fresh target') RETURNING id",Long.class);
        long targetCurrency=dst.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,target);
        long targetDefinition=dst.queryForObject("""
                INSERT INTO reward_definitions(family_id,title,point_type_id,price_amount)
                VALUES (?,'Reward',?,10) RETURNING id
                """,Long.class,target,targetCurrency);
        assertNotEquals(family,target);

        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(src,
                new WorkspaceResetSnapshotService(src,new WorkspaceEpochTransactionGuard(src)),
                codec,portable);
        var keyGen=KeyGenerator.getInstance("AES");
        keyGen.init(256);
        var key=keyGen.generateKey();
        var sourceTransaction=new TransactionTemplate(new DataSourceTransactionManager(sds));
        byte[] archive=sourceTransaction.execute(status->exporter.export(
                family,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("REWARD_CATEGORIES","REWARD_DEFINITIONS",
                        "REWARD_DEFINITION_CATEGORIES"),"V50",key)).encryptedFile();
        var replayer=new DisposableRewardCategoryRestoreExecutor(dst,
                new DataSourceTransactionManager(tds),inspector,portable,codec);
        var nullMapping=new HashMap<Long,Long>();
        nullMapping.put(definition,null);
        assertThrows(SecurityException.class,()->replayer.apply(
                archive,family,target,nullMapping,key));
        assertEquals(0L,dst.queryForObject(
                "SELECT count(*) FROM reward_categories WHERE family_id=?",
                Long.class,target));
        var replay=replayer.apply(archive,family,target,Map.of(definition,targetDefinition),key);
        assertEquals(1,replay.categoryIds().size());
        assertEquals(1,replay.links());
        long mapped=replay.categoryIds().get(category);
        assertNotEquals(category,mapped);
        assertEquals("Learning",dst.queryForObject(
                "SELECT name FROM reward_categories WHERE id=? AND family_id=?",
                String.class,mapped,target));
        assertEquals(1L,dst.queryForObject("""
                SELECT count(*) FROM reward_definition_categories
                WHERE reward_definition_id=? AND category_id=?
                """,Long.class,targetDefinition,mapped));
        assertEquals("Neighbor category",dst.queryForObject(
                "SELECT name FROM reward_categories WHERE id=?",
                String.class,neighborCategory));
        assertEquals(1L,dst.queryForObject("""
                SELECT count(*) FROM reward_definition_categories
                WHERE reward_definition_id=? AND category_id=?
                """,Long.class,neighborDefinition,neighborCategory));
        assertThrows(IllegalStateException.class,()->replayer.apply(
                archive,family,target,Map.of(definition,targetDefinition),key));
        assertEquals(1L,dst.queryForObject(
                "SELECT count(*) FROM reward_categories WHERE family_id=?",
                Long.class,target));
        long wrongTarget=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Wrong target') RETURNING id",Long.class);
        assertThrows(SecurityException.class,()->replayer.apply(
                archive,family,wrongTarget,Map.of(definition,neighborDefinition),key));
        assertEquals(0L,dst.queryForObject(
                "SELECT count(*) FROM reward_categories WHERE family_id=?",
                Long.class,wrongTarget));
        var corrupted=archive.clone();
        corrupted[corrupted.length-1]^=1;
        assertThrows(RuntimeException.class,()->replayer.apply(
                corrupted,family,wrongTarget,Map.of(definition,targetDefinition),key));
    }
}
