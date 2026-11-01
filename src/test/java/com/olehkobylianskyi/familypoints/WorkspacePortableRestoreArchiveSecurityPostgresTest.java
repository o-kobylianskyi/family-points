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

@Testcontainers
class WorkspacePortableRestoreArchiveSecurityPostgresTest {
    @Container static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("archive_security").withUsername("qa").withPassword("qa_password");

    @Test void wrongKeyTamperMalformedEnvelopeAndForeignWorkspaceNeverMutate() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Encrypted archive A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Encrypted archive B') RETURNING id",Long.class);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'A original',true,false)",a);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'B original',true,false)",b);
        var guard=new WorkspaceEpochTransactionGuard(db);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,guard),codec,portable);
        var preview=new WorkspacePortableRestorePreviewService(db,guard,inspector);
        var generator=KeyGenerator.getInstance("AES");
        generator.init(256);
        var key=generator.generateKey();
        var wrongKey=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        byte[] archive=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEMBER_GROUPS"),"V50",key)).encryptedFile();

        assertThrows(RuntimeException.class,()->tx.execute(status->
                preview.preview(archive,a,0L,List.of("MEMBER_GROUPS"),wrongKey)));
        byte[] tampered=archive.clone();
        tampered[tampered.length-1]^=1;
        assertThrows(RuntimeException.class,()->tx.execute(status->
                preview.preview(tampered,a,0L,List.of("MEMBER_GROUPS"),key)));
        byte[] trailing=Arrays.copyOf(archive,archive.length+1);
        assertThrows(IllegalArgumentException.class,()->tx.execute(status->
                preview.preview(trailing,a,0L,List.of("MEMBER_GROUPS"),key)));
        byte[] invalidMagic=archive.clone();
        invalidMagic[0]^=1;
        assertThrows(IllegalArgumentException.class,()->tx.execute(status->
                preview.preview(invalidMagic,a,0L,List.of("MEMBER_GROUPS"),key)));
        assertThrows(SecurityException.class,()->tx.execute(status->
                preview.preview(archive,b,0L,List.of("MEMBER_GROUPS"),key)));

        assertEquals("A original",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,a));
        assertEquals("B original",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,b));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,a));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,b));
    }
}
