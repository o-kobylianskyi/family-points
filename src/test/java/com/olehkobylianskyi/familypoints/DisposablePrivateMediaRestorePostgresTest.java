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
class DisposablePrivateMediaRestorePostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("private_media_roundtrip").withUsername("tester").withPassword("pass");

    @Test void restoresActualMediaBytesAndRemapsForeignKeysWithoutCrossFamilyWrites() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long source=db.queryForObject("INSERT INTO families(name) VALUES ('Media source') RETURNING id",Long.class);
        long target=db.queryForObject("INSERT INTO families(name) VALUES ('Media target') RETURNING id",Long.class);
        long neighbor=db.queryForObject("INSERT INTO families(name) VALUES ('Media neighbor') RETURNING id",Long.class);
        long srcRole=db.queryForObject("INSERT INTO family_roles(family_id,code,name,system_role) VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,source);
        long dstRole=db.queryForObject("INSERT INTO family_roles(family_id,code,name,system_role) VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,target);
        long srcMember=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,name,active,member_type) VALUES (?,?,'Source owner',true,'PARENT') RETURNING id",Long.class,source,srcRole);
        long dstMember=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,name,active,member_type) VALUES (?,?,'Target owner',true,'PARENT') RETURNING id",Long.class,target,dstRole);
        long srcGroup=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Source group',true,false) RETURNING id",Long.class,source);
        long dstGroup=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Target group',true,false) RETURNING id",Long.class,target);
        byte[] content=new byte[]{1,4,9,16,25,36};
        UUID asset=UUID.randomUUID();
        db.update("INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id) VALUES (?,?,'image/png',1,1,?,?,?)",
                asset,source,content.length,ProtectiveBackupArtifactCodec.sha(content),srcMember);
        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?,'COVER',?)",asset,content);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id) VALUES (?,'MEMBER_GROUP',?,'COVER',?)",source,srcGroup,asset);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,
                new WorkspaceResetSnapshotService(db,new WorkspaceEpochTransactionGuard(db)),codec,portable);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);var key=generator.generateKey();
        var manager=new DataSourceTransactionManager(ds);
        var tx=new TransactionTemplate(manager);
        byte[] archive=tx.execute(status->exporter.export(source,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEDIA_ASSETS","MEDIA_BINDINGS"),"V50",key)).encryptedFile();
        var executor=new DisposablePrivateMediaRestoreExecutor(db,manager,inspector,portable,codec);
        var result=executor.apply(archive,source,target,Map.of(srcMember,dstMember),
                Map.of(srcGroup,dstGroup),key);
        assertEquals(1,result.blobs());
        assertEquals(1,result.bindings());
        UUID newAsset=result.assetIds().get(asset);
        assertNotNull(newAsset);
        assertNotEquals(asset,newAsset);
        assertArrayEquals(content,db.queryForObject(
                "SELECT bytes FROM media_blobs WHERE asset_id=?",byte[].class,newAsset));
        assertEquals(dstMember,db.queryForObject(
                "SELECT created_by_member_id FROM media_assets WHERE id=?",Long.class,newAsset));
        assertEquals(dstGroup,db.queryForObject(
                "SELECT target_id FROM media_bindings WHERE workspace_id=? AND asset_id=?",
                Long.class,target,newAsset));
        assertThrows(IllegalStateException.class,()->executor.apply(archive,source,target,
                Map.of(srcMember,dstMember),Map.of(srcGroup,dstGroup),key));
        assertThrows(SecurityException.class,()->executor.apply(archive,source,neighbor,
                Map.of(srcMember,dstMember),Map.of(srcGroup,dstGroup),key));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,source));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,target));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,neighbor));
    }
}
