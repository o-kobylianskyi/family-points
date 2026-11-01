package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import javax.crypto.KeyGenerator;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Genuine native-schema disposable restore for one explicitly mapped category.
 * Never exposed as an HTTP endpoint or callable production restore operation.
 */
@Testcontainers
class WorkspacePortableNativeApplyPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("native_apply").withUsername("rehearsal").withPassword("rehearsal_pass");

    @Test void decryptAndApplyGroupsToNativeSchemaWithIdRemapAndRollback() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long source=db.queryForObject("INSERT INTO families(name) VALUES ('Source') RETURNING id",Long.class);
        long target=db.queryForObject("INSERT INTO families(name) VALUES ('Disposable target') RETURNING id",Long.class);
        long neighbor=db.queryForObject("INSERT INTO families(name) VALUES ('Neighbor') RETURNING id",Long.class);
        long original=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Archive group',true,false) RETURNING id",Long.class,source);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Protected neighbor',true,false)",neighbor);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key) "+
                "VALUES (?,'MEMBER_GROUP',?,'ICON','group-default')",source,original);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var guard=new WorkspaceEpochTransactionGuard(db);
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,guard),codec,portable);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);var key=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        byte[] file=tx.execute(status->exporter.export(source,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS"),"V50",key)).encryptedFile();
        assertEquals(1,inspector.inspect(file,source,key).counts().get("MEMBER_GROUPS"));
        var envelope=portable.decode(file);
        byte[] clear=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
        var record=new ObjectMapper().readTree(clear).path("categories").path("MEMBER_GROUPS").get(0);
        Arrays.fill(clear,(byte)0);
        assertEquals(original,record.path("id").asLong());
        assertEquals(source,record.path("family_id").asLong());

        var nativeRestorer=new WorkspaceDisposableNativeGroupRestorer(db,
                new DataSourceTransactionManager(ds),inspector,portable,codec);
        var proof=nativeRestorer.rehearse(file,source,target,key);
        assertTrue(proof.rolledBack());
        assertEquals(1,proof.inserted());
        assertTrue(proof.groupIdMapping().containsKey(original));
        assertNotEquals(original,proof.groupIdMapping().get(original));
        assertThrows(IllegalArgumentException.class,
                ()->nativeRestorer.rehearse(file,source,source,key));
        assertThrows(SecurityException.class,
                ()->nativeRestorer.rehearse(file,neighbor,target,key));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,target));

        byte[] mixed=tx.execute(status->exporter.export(source,UUID.randomUUID(),
                UUID.randomUUID(),0L,List.of("MEMBER_GROUPS","MEDIA_BINDINGS"),"V50",key))
                .encryptedFile();
        var mediaProof=nativeRestorer.rehearse(mixed,source,target,key);
        assertEquals(1,mediaProof.inserted());
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,target));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,source));

        // Real table insert (not JSON staging). The source PK cannot be reused
        // in the destination family: return a fresh PK and assert its mapping.
        var mapped=tx.execute(status->{
            long newId=db.queryForObject("""
                    INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                    VALUES (?,?,?,?) RETURNING id
                    """,Long.class,target,record.path("name").asText(),
                    record.path("active").asBoolean(),record.path("show_in_navigation").asBoolean());
            assertNotEquals(original,newId);
            assertEquals("Archive group",db.queryForObject(
                    "SELECT name FROM member_groups WHERE id=? AND family_id=?",
                    String.class,newId,target));
            assertEquals(1L,db.queryForObject(
                    "SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,target));
            status.setRollbackOnly();
            return Map.of(original,newId);
        });
        assertTrue(mapped.containsKey(original));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,target));
        assertEquals("Archive group",db.queryForObject("SELECT name FROM member_groups WHERE id=?",String.class,original));
        assertEquals("Protected neighbor",db.queryForObject(
                "SELECT name FROM member_groups WHERE family_id=?",String.class,neighbor));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,target));
    }
}
