package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import javax.crypto.KeyGenerator;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspacePortableBackupExporterPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("portable_export").withUsername("exporter")
            .withPassword("exporter_password");

    @Test void exportsOnlySelectedFamiliesAndDoesNotMakeArchiveRestorable() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long a=db.queryForObject("INSERT INTO families(name) VALUES ('Archive A') RETURNING id",Long.class);
        long b=db.queryForObject("INSERT INTO families(name) VALUES ('Archive B') RETURNING id",Long.class);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?, 'Kids A',true,false)",a);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?, 'Private B',true,false)",b);
        var guard=new WorkspaceEpochTransactionGuard(db);
        var snapshot=new WorkspaceResetSnapshotService(db,guard);
        var codec=new ProtectiveBackupArtifactCodec();
        var files=new ProtectiveBackupPortableFile();
        var exporter=new WorkspacePortableBackupExporter(db,snapshot,codec,files);
        var inspector=new ProtectiveBackupImportInspector(files,codec);
        var keyGen=KeyGenerator.getInstance("AES");keyGen.init(256);var key=keyGen.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        UUID snap=UUID.randomUUID();
        var exported=tx.execute(s->exporter.export(a,snap,UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS"),"V50",key));
        assertNotNull(exported);
        assertEquals(1,exported.recordCounts().get("MEMBER_GROUPS"));
        assertTrue(exported.byteCount()>0);
        var inspected=inspector.inspect(exported.encryptedFile(),a,key);
        assertEquals(1,inspected.counts().get("MEMBER_GROUPS"));
        // Export is read-only, but its source fingerprint must correspond to
        // exactly the persisted family snapshot, not just matching counts.
        assertEquals(exported.contentDigest(),
                tx.execute(status->snapshot.digest(a,List.of("MEMBER_GROUPS"))));
        db.update("UPDATE member_groups SET name='Kids A renamed' WHERE family_id=?",a);
        var changed=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEMBER_GROUPS"),"V50",key));
        assertNotEquals(exported.contentDigest(),changed.contentDigest(),
                "A same-count value change must invalidate the previous export fingerprint");
        assertEquals(1,inspector.inspect(changed.encryptedFile(),a,key)
                .counts().get("MEMBER_GROUPS"));

        assertFalse(inspected.restorable());
        assertThrows(SecurityException.class,()->inspector.inspect(exported.encryptedFile(),b,key));
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->tx.execute(s->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),0L,
                        List.of("POINT_LEDGER"),"V50",key))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(s->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),7L,
                        List.of("MEMBER_GROUPS"),"V50",key))).getStatusCode());
        var supported=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("TASK_DEFINITIONS","REWARD_DEFINITIONS","MEMBER_GROUPS",
                        "PRIVATE_TEMPLATES","TASK_EXECUTIONS","TASK_SCHEDULE_SLOTS",
                        "REWARD_PURCHASES"),"V50",key));
        var seven=inspector.inspect(supported.encryptedFile(),a,key);
        assertEquals(7,seven.availableCategories().size());
        assertEquals(0,seven.counts().get("TASK_DEFINITIONS"));
        assertEquals(0,seven.counts().get("REWARD_DEFINITIONS"));
        assertEquals(0,seven.counts().get("TASK_EXECUTIONS"));
        assertEquals(0,seven.counts().get("TASK_SCHEDULE_SLOTS"));
        assertEquals(0,seven.counts().get("REWARD_PURCHASES"));
        assertFalse(seven.restorable(),"An exported archive is not yet a verified restore");
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,a));
        assertEquals("Private B",db.queryForObject(
                "SELECT name FROM member_groups WHERE family_id=?",String.class,b));
    }
}
