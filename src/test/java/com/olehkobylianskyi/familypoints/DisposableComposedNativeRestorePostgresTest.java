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
 * End-to-end native group + private media COMMIT in one database transaction.
 * All code executes exclusively in disposable PostgreSQL under src/test.
 */
@Testcontainers
class DisposableComposedNativeRestorePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("composed_restore").withUsername("restorer").withPassword("restorer_pass");

    @Test void groupAndOwnedBinaryMediaCommitTogetherAndFailedReplayIsAtomic() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long source=db.queryForObject("INSERT INTO families(name) VALUES ('Source') RETURNING id",Long.class);
        long target=db.queryForObject("INSERT INTO families(name) VALUES ('Target') RETURNING id",Long.class);
        long neighbor=db.queryForObject("INSERT INTO families(name) VALUES ('Neighbor') RETURNING id",Long.class);
        long srcRole=db.queryForObject("INSERT INTO family_roles(family_id,code,name,system_role) VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,source);
        long dstRole=db.queryForObject("INSERT INTO family_roles(family_id,code,name,system_role) VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,target);
        long srcMember=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,name,active,member_type) VALUES (?,?,'Source parent',true,'PARENT') RETURNING id",Long.class,source,srcRole);
        long dstMember=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,name,active,member_type) VALUES (?,?,'Target parent',true,'PARENT') RETURNING id",Long.class,target,dstRole);
        long srcGroup=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Media group',true,false) RETURNING id",Long.class,source);
        byte[] bytes=new byte[]{10,20,30,40,50,60};
        UUID srcAsset=UUID.randomUUID();
        db.update("INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id) VALUES (?,?,'image/png',1,1,?,?,?)",
                srcAsset,source,bytes.length,ProtectiveBackupArtifactCodec.sha(bytes),srcMember);
        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?,'COVER',?)",srcAsset,bytes);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id) VALUES (?,'MEMBER_GROUP',?,'COVER',?)",
                source,srcGroup,srcAsset);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,
                new WorkspaceEpochTransactionGuard(db)),codec,portable);
        var keyGen=KeyGenerator.getInstance("AES");keyGen.init(256);var key=keyGen.generateKey();
        var manager=new DataSourceTransactionManager(ds);
        var tx=new TransactionTemplate(manager);
        byte[] groups=tx.execute(status->exporter.export(source,UUID.randomUUID(),
                UUID.randomUUID(),0L,List.of("MEMBER_GROUPS"),"V50",key)).encryptedFile();
        byte[] media=tx.execute(status->exporter.export(source,UUID.randomUUID(),
                UUID.randomUUID(),0L,List.of("MEDIA_ASSETS","MEDIA_BINDINGS"),"V50",key)).encryptedFile();
        byte[] unified=tx.execute(status->exporter.export(source,UUID.randomUUID(),
                UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS","MEDIA_ASSETS","MEDIA_BINDINGS"),"V50",key))
                .encryptedFile();
        var unifiedProof=inspector.inspect(unified,source,key);
        assertEquals(1,unifiedProof.counts().get("MEMBER_GROUPS"));
        assertEquals(1,unifiedProof.counts().get("MEDIA_ASSETS"));
        assertEquals(1,unifiedProof.counts().get("MEDIA_BINDINGS"));
        var decoded=portable.decode(unified);
        byte[] unifiedPlain=codec.open(decoded,decoded.manifest().snapshotId(),source,key);
        try {
            var categories=new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(unifiedPlain).path("categories");
            assertEquals(3,categories.size());
        } finally {Arrays.fill(unifiedPlain,(byte)0);}
        var groupEngine=new DisposableNativeRestoreExecutor(db,manager,portable,codec,inspector);
        var mediaEngine=new DisposablePrivateMediaRestoreExecutor(db,manager,inspector,portable,codec);
        var restored=tx.execute(status->{
            var g=groupEngine.apply(groups,source,target,key);
            var m=mediaEngine.apply(media,source,target,
                    Map.of(srcMember,dstMember),g.groupIds(),key);
            return m;
        });
        assertNotNull(restored);
        assertEquals(1,restored.blobs());
        assertEquals(1,restored.bindings());
        var newAsset=restored.assetIds().get(srcAsset);
        assertArrayEquals(bytes,db.queryForObject(
                "SELECT bytes FROM media_blobs WHERE asset_id=?",byte[].class,newAsset));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,target));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,target));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,target));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,source));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,neighbor));
        // One authenticated FULL media/group archive, one atomic outer transaction.
        // The test-only composition reuses the existing native SQL executors.
        var unifiedEngine=new DisposableUnifiedGroupMediaRestoreExecutor(
                db,manager,portable,codec,inspector);
        long unifiedTarget=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Unified target') RETURNING id",Long.class);
        long unifiedRole=db.queryForObject(
                "INSERT INTO family_roles(family_id,code,name,system_role) "
                +"VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,unifiedTarget);
        long unifiedMember=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,name,active,member_type) "
                +"VALUES (?,?,'Unified owner',true,'PARENT') RETURNING id",
                Long.class,unifiedTarget,unifiedRole);
        var unifiedResult=unifiedEngine.apply(unified,source,unifiedTarget,
                Map.of(srcMember,unifiedMember),key);
        assertEquals(1,unifiedResult.groupIds().size());
        assertEquals(1,unifiedResult.blobs());
        assertEquals(1,unifiedResult.privateBindings());
        assertEquals(0,unifiedResult.catalogBindings());
        assertArrayEquals(bytes,db.queryForObject(
                "SELECT bytes FROM media_blobs WHERE asset_id=?",byte[].class,
                unifiedResult.assetIds().get(srcAsset)));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,unifiedTarget));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM media_bindings WHERE workspace_id=?",
                Long.class,unifiedTarget));

        long rejectedTarget=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Rejected unified') RETURNING id",Long.class);
        assertThrows(SecurityException.class,()->unifiedEngine.apply(
                unified,source,rejectedTarget,Map.of(srcMember,dstMember),key));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,rejectedTarget));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM media_assets WHERE workspace_id=?",
                Long.class,rejectedTarget));
        assertEquals(0L,db.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?",
                Long.class,rejectedTarget));
        long freshTarget=db.queryForObject("INSERT INTO families(name) VALUES ('Invalid target') RETURNING id",Long.class);
        assertThrows(SecurityException.class,()->tx.execute(status->{
            var g=groupEngine.apply(groups,source,freshTarget,key);
            mediaEngine.apply(media,source,freshTarget,
                    Map.of(srcMember,dstMember),g.groupIds(),key);
            return null;
        }));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,freshTarget));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,freshTarget));
    }
}
