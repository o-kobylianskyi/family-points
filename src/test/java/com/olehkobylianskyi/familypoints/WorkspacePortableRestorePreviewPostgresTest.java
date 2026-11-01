package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import javax.crypto.KeyGenerator;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspacePortableRestorePreviewPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("restore_preview").withUsername("preview").withPassword("preview_password");

    @Test void reportsTargetConflictsAndNeverChangesEitherWorkspace() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Family A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Family B') RETURNING id",Long.class);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Kids',true,false)",a);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Others',true,false)",b);
        var guard=new WorkspaceEpochTransactionGuard(db);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,guard),codec,portable);
        var preview=new WorkspacePortableRestorePreviewService(db,guard,inspector);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);var key=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        byte[] archive=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEMBER_GROUPS"),"V50",key)).encryptedFile();
        var result=tx.execute(status->preview.preview(archive,a,0L,List.of("MEMBER_GROUPS"),key));
        assertFalse(result.executable());
        assertFalse(result.blockers().contains("RESTORE_SCHEMA_VERSION_MISMATCH"));
        var decoded=portable.decode(archive);
        byte[] plain=codec.open(decoded,decoded.manifest().snapshotId(),a,key);
        var oldSealed=codec.seal(UUID.randomUUID(),a,UUID.randomUUID(),0L,
                decoded.manifest().sourceDigest(),"V49",plain,key);
        var oldPreview=tx.execute(status->preview.preview(portable.encode(oldSealed),a,0L,
                List.of("MEMBER_GROUPS"),key));
        assertFalse(oldPreview.executable());
        assertTrue(oldPreview.blockers().contains("RESTORE_SCHEMA_VERSION_MISMATCH"));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,a));

        assertEquals(1L,result.categories().get(0).targetRows());
        assertEquals(1,result.categories().get(0).archiveRows());
        assertTrue(result.blockers().contains("RESTORE_TARGET_CONFLICT:MEMBER_GROUPS"));
        assertTrue(result.blockers().contains("RESTORE_FK_REMAP_UNVERIFIED:MEMBER_GROUPS"));
        assertThrows(SecurityException.class,
                ()->tx.execute(status->preview.preview(archive,b,0L,List.of("MEMBER_GROUPS"),key)));
        var allSelected=List.of("PRIVATE_TEMPLATES","MEMBER_GROUPS","TASK_DEFINITIONS",
                "REWARD_DEFINITIONS","REWARD_REQUESTS","WORK_NODES","MEDIA_BINDINGS",
                "TASK_EXECUTIONS","TASK_SCHEDULE_SLOTS","REWARD_PURCHASES",
                "POINT_LEDGER","REPUTATION_LEDGER");
        byte[] fullSlice=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,allSelected,"V50",key)).encryptedFile();
        var previewSlice=tx.execute(status->preview.preview(fullSlice,a,0L,allSelected,key));
        assertEquals(12,previewSlice.categories().size());
        assertTrue(previewSlice.categories().stream().allMatch(row->row.targetRows()>=0));
        assertFalse(previewSlice.executable());
        assertTrue(previewSlice.blockers().contains("RESTORE_TARGET_CONFLICT:MEMBER_GROUPS"));
        assertEquals("Others",db.queryForObject(
                "SELECT name FROM member_groups WHERE family_id=?",String.class,b));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,a));
        assertEquals("Others",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,b));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,a));
        assertThrows(ResponseStatusException.class,
                ()->tx.execute(status->preview.preview(archive,a,2L,List.of("MEMBER_GROUPS"),key)));
    }

    @Test void rejectsMissingSelectionAbsentCategoryAndUnsupportedMappingWithoutWrites() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Preview source') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Untouched second family') RETURNING id",Long.class);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Export group',true,false)",a);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Keep group',true,false)",b);
        var guard=new WorkspaceEpochTransactionGuard(db);
        var codec=new ProtectiveBackupArtifactCodec();
        var file=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(file,codec);
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,guard),codec,file);
        var preview=new WorkspacePortableRestorePreviewService(db,guard,inspector);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);var key=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        byte[] archive=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEMBER_GROUPS","POINT_LEDGER"),"V50",key)).encryptedFile();

        var noSelection=assertThrows(ResponseStatusException.class,
                ()->tx.execute(status->preview.preview(archive,a,0L,List.of(),key)));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST,noSelection.getStatusCode());
        var notInArchive=assertThrows(ResponseStatusException.class,
                ()->tx.execute(status->preview.preview(archive,a,0L,List.of("PRIVATE_TEMPLATES"),key)));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST,notInArchive.getStatusCode());
        var result=tx.execute(status->preview.preview(archive,a,0L,List.of("POINT_LEDGER"),key));
        assertFalse(result.executable());
        assertEquals(1,result.categories().size());
        assertEquals(0L,result.categories().get(0).targetRows());
        assertFalse(result.blockers().contains("RESTORE_CATEGORY_MAPPING_REQUIRED:POINT_LEDGER"));
        assertTrue(result.blockers().contains("RESTORE_FK_REMAP_UNVERIFIED:POINT_LEDGER"));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,a));
        assertEquals("Export group",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,a));
        assertEquals("Keep group",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,b));
    }
}
