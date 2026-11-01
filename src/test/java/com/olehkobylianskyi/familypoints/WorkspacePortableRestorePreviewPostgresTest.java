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
        assertEquals(1L,result.categories().get(0).targetRows());
        assertEquals(1,result.categories().get(0).archiveRows());
        assertTrue(result.blockers().contains("RESTORE_TARGET_CONFLICT:MEMBER_GROUPS"));
        assertTrue(result.blockers().contains("RESTORE_FK_REMAP_UNVERIFIED:MEMBER_GROUPS"));
        assertThrows(SecurityException.class,
                ()->tx.execute(status->preview.preview(archive,b,0L,List.of("MEMBER_GROUPS"),key)));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,a));
        assertEquals("Others",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,b));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,a));
        assertThrows(ResponseStatusException.class,
                ()->tx.execute(status->preview.preview(archive,a,2L,List.of("MEMBER_GROUPS"),key)));
    }
}
