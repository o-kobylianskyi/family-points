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
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key) "+
                "VALUES (?,'WORKSPACE',?,'COVER','avatar-test')",a,a);
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
        var ledgerExport=tx.execute(status->exporter.export(a,UUID.randomUUID(),
                UUID.randomUUID(),0L,List.of("POINT_LEDGER"),"V50",key));
        assertEquals(0,inspector.inspect(ledgerExport.encryptedFile(),a,key)
                .counts().get("POINT_LEDGER"));
        // Real family-owned image with an actual binary variant, not an empty
        // media-assets table. This is the data recovery smoke test.
        long mediaRole=db.queryForObject(
                "INSERT INTO family_roles(family_id,code,name,system_role) "+
                "VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,a);
        long mediaMember=db.queryForObject(
                "INSERT INTO family_members(family_id,family_role_id,name,active,member_type) "+
                "VALUES (?,?,'Image owner',true,'PARENT') RETURNING id",Long.class,a,mediaRole);
        UUID mediaId=UUID.randomUUID();
        byte[] imageBytes=new byte[]{(byte)137,80,78,71,13,10,26,10,1,2,3,4};
        String imageDigest=ProtectiveBackupArtifactCodec.sha(imageBytes);
        db.update("INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id) "+
                "VALUES (?,?,'image/png',1,1,?,?,?)",
                mediaId,a,imageBytes.length,imageDigest,mediaMember);
        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?,'COVER',?)",mediaId,imageBytes);
        var mediaOnly=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEDIA_ASSETS"),"V50",key));
        assertEquals(1,inspector.inspect(mediaOnly.encryptedFile(),a,key)
                .counts().get("MEDIA_ASSETS"));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->tx.execute(s->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),7L,
                        List.of("MEMBER_GROUPS"),"V50",key))).getStatusCode());
        var mediaArchive=tx.execute(status->exporter.export(a,UUID.randomUUID(),
                UUID.randomUUID(),0L,List.of("MEDIA_ASSETS","MEDIA_BINDINGS"),"V50",key));
        var mediaInspection=inspector.inspect(mediaArchive.encryptedFile(),a,key);
        assertEquals(1,mediaInspection.counts().get("MEDIA_ASSETS"));
        var decrypted=files.decode(mediaArchive.encryptedFile());
        var content=new String(codec.open(decrypted,decrypted.manifest().snapshotId(),a,key),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains(Base64.getEncoder().encodeToString(imageBytes)),
                "Actual media blob bytes must survive encryption and file roundtrip");
        assertEquals(1,mediaInspection.counts().get("MEDIA_BINDINGS"));
        assertFalse(mediaInspection.restorable());
        byte[] decodedBytes=Base64.getDecoder().decode(new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(content).path("categories").path("MEDIA_ASSETS").get(0)
                .path("blobs").get(0).path("bytesBase64").asText());
        assertArrayEquals(imageBytes,decodedBytes);
        // Transaction-local rehearsal: stage decrypted bytes into an isolated
        // PostgreSQL temporary table, verify checksum, and force rollback.
        // Never write restored values into family-owned production tables.
        tx.execute(status->{
            db.execute("CREATE TEMP TABLE fp_backup_rehearsal(asset_id uuid,variant text,bytes bytea) ON COMMIT DROP");
            db.update("INSERT INTO fp_backup_rehearsal(asset_id,variant,bytes) VALUES (?,'COVER',?)",
                    mediaId,decodedBytes);
            byte[] restored=db.queryForObject(
                    "SELECT bytes FROM fp_backup_rehearsal WHERE asset_id=?",byte[].class,mediaId);
            assertArrayEquals(imageBytes,restored);
            assertEquals(imageDigest,ProtectiveBackupArtifactCodec.sha(restored));
            status.setRollbackOnly();
            return null;
        });
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM media_blobs WHERE asset_id=?",
                Long.class,mediaId));
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
        var extended=tx.execute(status->exporter.export(a,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("REWARD_REQUESTS","WORK_NODES","MEDIA_BINDINGS",
                        "POINT_LEDGER","REPUTATION_LEDGER"),"V50",key));
        var extendedInspection=inspector.inspect(extended.encryptedFile(),a,key);
        assertEquals(5,extendedInspection.availableCategories().size());
        for(String category:extendedInspection.availableCategories())
            assertEquals("MEDIA_BINDINGS".equals(category)?1:0,
                    extendedInspection.counts().get(category),category);
        assertFalse(extendedInspection.restorable(),
                "Read-only financial archive does not authorize ledger deletion or restore");

        assertEquals(1L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,a));
        assertEquals("Private B",db.queryForObject(
                "SELECT name FROM member_groups WHERE family_id=?",String.class,b));
    }
}
