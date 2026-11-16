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

/**
 * Independent source/target PostgreSQL 18; native ledger reconstruction on a
 * disposable target only. No production endpoint, owner proof or VERIFIED.
 */
@Testcontainers
class DisposableGroupLedgerCrossDatabasePostgresTest {
    @Container static final PostgreSQLContainer SOURCE =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("fp_group_ledger_source")
                    .withUsername("source").withPassword("isolated_test_source");
    @Container static final PostgreSQLContainer TARGET =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("fp_group_ledger_target")
                    .withUsername("target").withPassword("isolated_test_target");

    @Test void manualLedgerReplayedWithRemappedKeysAndBalancedHistory() throws Exception {
        var sourceDs=new DriverManagerDataSource(
                SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var targetDs=new DriverManagerDataSource(
                TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(sourceDs).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(targetDs).locations("classpath:db/migration").load().migrate();
        var src=new JdbcTemplate(sourceDs);
        var dst=new JdbcTemplate(targetDs);
        long source=src.queryForObject(
                "INSERT INTO families(name) VALUES ('Source wallet') RETURNING id",Long.class);
        long oldGroup=src.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Source group',true,false) RETURNING id
                """,Long.class,source);
        long oldCurrency=src.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,source);
        src.update("""
                INSERT INTO group_point_transactions
                  (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,15,NOW(),'EARN','MANUAL','Earned')
                """,oldGroup,oldCurrency);
        src.update("""
                INSERT INTO group_point_transactions
                  (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,-4,NOW(),'SPEND','MANUAL','Spent')
                """,oldGroup,oldCurrency);

        long neighbor=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Neighbor') RETURNING id",Long.class);
        long neighborGroup=dst.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Neighbor group',true,false) RETURNING id
                """,Long.class,neighbor);
        long neighborCurrency=dst.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,neighbor);
        dst.update("""
                INSERT INTO group_point_transactions
                  (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,99,NOW(),'EARN','MANUAL','Neighbor unchanged')
                """,neighborGroup,neighborCurrency);
        long target=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Fresh target') RETURNING id",Long.class);
        long targetGroup=dst.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Remapped group',true,false) RETURNING id
                """,Long.class,target);
        long targetCurrency=dst.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,target);
        assertNotEquals(source,target);
        assertNotEquals(oldGroup,targetGroup);
        assertNotEquals(oldCurrency,targetCurrency);
        assertEquals("POINTS",src.queryForObject(
                "SELECT code FROM point_types WHERE id=?",String.class,oldCurrency));
        assertEquals("POINTS",dst.queryForObject(
                "SELECT code FROM point_types WHERE id=?",String.class,targetCurrency));

        var codec=new ProtectiveBackupArtifactCodec();
        var files=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(files,codec);
        var exporter=new WorkspacePortableBackupExporter(src,
                new WorkspaceResetSnapshotService(src,new WorkspaceEpochTransactionGuard(src)),
                codec,files);
        var keyGen=KeyGenerator.getInstance("AES");
        keyGen.init(256);
        var key=keyGen.generateKey();
        var sourceTx=new TransactionTemplate(new DataSourceTransactionManager(sourceDs));
        byte[] archive=sourceTx.execute(status->exporter.export(
                source,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS","GROUP_POINT_LEDGER"),"V50",key)).encryptedFile();

        var engine=new DisposableGroupLedgerRestoreExecutor(dst,
                new DataSourceTransactionManager(targetDs),files,codec,inspector);
        var result=engine.apply(archive,source,target,
                Map.of(oldGroup,targetGroup),Map.of(oldCurrency,targetCurrency),key);
        assertEquals(2,result.replayed());
        assertEquals(2,result.transactionIds().size());
        assertEquals(11L,result.balancesByTargetGroup().get(targetGroup).longValue());
        assertEquals(11L,dst.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM group_point_transactions WHERE group_id=?
                """,Long.class,targetGroup).longValue());
        assertEquals(99L,dst.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM group_point_transactions WHERE group_id=?
                """,Long.class,neighborGroup).longValue());
        assertEquals(11L,src.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM group_point_transactions WHERE group_id=?
                """,Long.class,oldGroup).longValue());
        assertThrows(IllegalStateException.class,()->engine.apply(archive,source,target,
                Map.of(oldGroup,targetGroup),Map.of(oldCurrency,targetCurrency),key));
        assertEquals(2L,dst.queryForObject(
                "SELECT count(*) FROM group_point_transactions WHERE group_id=?",
                Long.class,targetGroup).longValue());
        long wrongTarget=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Wrong target') RETURNING id",Long.class);
        assertThrows(SecurityException.class,()->engine.apply(archive,source,wrongTarget,
                Map.of(oldGroup,neighborGroup),Map.of(oldCurrency,targetCurrency),key));
        assertThrows(SecurityException.class,()->engine.apply(archive,source,wrongTarget,
                Map.of(oldGroup,targetGroup),Map.of(oldCurrency,neighborCurrency),key));
        assertThrows(SecurityException.class,()->inspector.inspect(archive,target,key));
    }
}
