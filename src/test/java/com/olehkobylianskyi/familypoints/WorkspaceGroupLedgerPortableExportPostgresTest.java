package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class WorkspaceGroupLedgerPortableExportPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("group_ledger_portable").withUsername("ledger")
            .withPassword("ledger_testing_only");

    @Test void exportedEncryptedGroupLedgerRetainsImmutableRowsAndRejectsNonClosedScope() throws Exception {
        var source=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(source);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Ledger family') RETURNING id",Long.class);
        long neighbor=db.queryForObject("INSERT INTO families(name) VALUES ('Other family') RETURNING id",Long.class);
        long group=db.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Pocket money group',true,false) RETURNING id
                """,Long.class,family);
        long currency=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,family);
        long otherGroup=db.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Foreign group',true,false) RETURNING id
                """,Long.class,neighbor);
        long otherCurrency=db.queryForObject("""
                INSERT INTO point_types(family_id,code,name,active,sort_order)
                VALUES (?,'POINTS','Points',true,0) RETURNING id
                """,Long.class,neighbor);
        db.update("""
                INSERT INTO group_point_transactions
                    (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,15,NOW(),'EARN','MANUAL','Original points')
                """,group,currency);
        db.update("""
                INSERT INTO group_point_transactions
                    (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,-4,NOW(),'SPEND','MANUAL','Original spend')
                """,group,currency);
        db.update("""
                INSERT INTO group_point_transactions
                    (group_id,point_type_id,amount,created_at,type,source_type,description)
                VALUES (?,?,99,NOW(),'EARN','MANUAL','Foreign points')
                """,otherGroup,otherCurrency);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,
                new WorkspaceResetSnapshotService(db,new WorkspaceEpochTransactionGuard(db)),
                codec,portable);
        var generator=KeyGenerator.getInstance("AES");
        generator.init(256);
        var key=generator.generateKey();
        var transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        UUID snapshot=UUID.randomUUID();
        byte[] file=transaction.execute(status->exporter.export(
                family,snapshot,UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS","GROUP_POINT_LEDGER"),"V50",key)).encryptedFile();

        var report=inspector.inspect(file,family,key);
        assertEquals(snapshot,report.snapshotId());
        assertEquals(2,report.counts().get("GROUP_POINT_LEDGER").intValue());
        assertEquals(1,report.counts().get("MEMBER_GROUPS").intValue());
        assertFalse(report.restorable());
        assertTrue(report.blockers().contains("CURRENCY_REBIND_REQUIRED:GROUP_POINT_LEDGER.point_type_id"));

        var envelope=portable.decode(file);
        byte[] decrypted=codec.open(envelope,snapshot,family,key);
        try {
            var categories=new ObjectMapper().readTree(decrypted).path("categories");
            var history=categories.path("GROUP_POINT_LEDGER");
            assertEquals(2,history.size());
            var amounts=new HashSet<Integer>();
            for(var item:history) {
                assertEquals(group,item.path("group_id").asLong());
                assertEquals(currency,item.path("point_type_id").asLong());
                amounts.add(item.path("amount").asInt());
            }
            assertEquals(Set.of(15,-4),amounts);
        } finally {Arrays.fill(decrypted,(byte)0);}
        assertEquals(11L,db.queryForObject("""
                SELECT sum(amount) FROM group_point_transactions WHERE group_id=?
                """,Long.class,group).longValue());
        assertEquals(99L,db.queryForObject("""
                SELECT sum(amount) FROM group_point_transactions WHERE group_id=?
                """,Long.class,otherGroup).longValue());
        assertThrows(SecurityException.class,()->inspector.inspect(file,neighbor,key));

        // No group category: inspector refuses a dangling group ledger graph,
        // even when the AES ciphertext itself is cryptographically valid.
        byte[] missingGroups=transaction.execute(status->exporter.export(
                family,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("GROUP_POINT_LEDGER"),"V50",key)).encryptedFile();
        var blocked=inspector.inspect(missingGroups,family,key);
        assertTrue(blocked.blockers().contains("MISSING_CATEGORY:MEMBER_GROUPS"));
        assertFalse(blocked.restorable());
    }
}
