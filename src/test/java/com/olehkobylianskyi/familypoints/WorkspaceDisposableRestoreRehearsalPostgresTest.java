package com.olehkobylianskyi.familypoints;

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

/** Exercises the actual reusable, rollback-only restore rehearsal service.
 * Never invokes a live restore or writes production family data.
 */
@Testcontainers
class WorkspaceDisposableRestoreRehearsalPostgresTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("rehearsal_contract").withUsername("rehearsal").withPassword("rehearsal_password");

    @Test void portableGroupMediaFileRoundtripsThroughRealRehearsalServiceWithoutMutations() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('A protected') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('B untouched') RETURNING id",Long.class);
        long group=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Media group',true,false) RETURNING id",Long.class,a);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Neighbor group',true,false)",b);
        long role=db.queryForObject("INSERT INTO family_roles(family_id,code,name,system_role) VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,a);
        long member=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,name,active,member_type) VALUES (?,?,'Media owner',true,'PARENT') RETURNING id",Long.class,a,role);
        UUID asset=UUID.randomUUID();
        byte[] binary=new byte[]{13,37,42,99,4,5,6,7,8};
        db.update("INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id) VALUES (?,?,'image/png',1,1,?,?,?)",
                asset,a,binary.length,ProtectiveBackupArtifactCodec.sha(binary),member);
        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?,'COVER',?)",asset,binary);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id) VALUES (?,'MEMBER_GROUP',?,'COVER',?)",a,group,asset);

        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,
                new WorkspaceResetSnapshotService(db,new WorkspaceEpochTransactionGuard(db)),codec,portable);
        var manager=new DataSourceTransactionManager(ds);
        var tx=new TransactionTemplate(manager);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);var key=generator.generateKey();
        var archive=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS","MEDIA_ASSETS","MEDIA_BINDINGS"),"V50",key)).encryptedFile();
        var rehearsal=new WorkspaceDisposableRestoreRehearsal(db,manager,portable,codec,inspector);

        var result=rehearsal.rehearse(archive,a,key);
        assertTrue(result.rolledBack());
        assertEquals(a,result.workspaceId());
        assertEquals(1,result.groups());
        assertEquals(1,result.assets());
        assertEquals(1,result.bindings());
        assertEquals(1,result.checkedVariants());
        assertThrows(SecurityException.class,()->rehearsal.rehearse(archive,b,key));
        assertThrows(IllegalArgumentException.class,()->rehearsal.rehearse(
                tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),0L,
                    List.of("MEMBER_GROUPS"),"V50",key)).encryptedFile(),a,key));

        assertEquals("Media group",db.queryForObject("SELECT name FROM member_groups WHERE id=?",String.class,group));
        assertEquals("Neighbor group",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,b));
        assertArrayEquals(binary,db.queryForObject("SELECT bytes FROM media_blobs WHERE asset_id=?",byte[].class,asset));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM media_bindings WHERE workspace_id=? AND asset_id=?",Long.class,a,asset));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,a));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,b));
    }
}
