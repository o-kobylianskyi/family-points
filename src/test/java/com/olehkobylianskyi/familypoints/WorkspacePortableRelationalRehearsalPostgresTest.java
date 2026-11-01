package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Archive -> isolated SQL staging -> FK validation -> rollback.
 * Staged rows never touch source family tables. This is a limited rehearsal,
 * not a complete, executable family restore nor VERIFIED evidence.
 */
@Testcontainers
class WorkspacePortableRelationalRehearsalPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("relational_rehearsal").withUsername("backup").withPassword("backup_password");

    @Test void stagesFamilyGroupsAndDetectsBrokenReferencesWithFullRollback() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Rehearsal source') RETURNING id",Long.class);
        long other=db.queryForObject("INSERT INTO families(name) VALUES ('Rehearsal neighbor') RETURNING id",Long.class);
        long parent=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Parent',true,false) RETURNING id",Long.class,family);
        long child=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Child',true,false) RETURNING id",Long.class,family);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Untouched neighbor',true,false)",other);
        var guard=new WorkspaceEpochTransactionGuard(db);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,guard),codec,portable);
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);var key=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var archive=tx.execute(status->exporter.export(family,UUID.randomUUID(),UUID.randomUUID(),
            0L,List.of("MEMBER_GROUPS"),"V50",key)).encryptedFile();
        assertEquals(2,inspector.inspect(archive,family,key).counts().get("MEMBER_GROUPS"));
        var envelope=portable.decode(archive);
        byte[] json=codec.open(envelope,envelope.manifest().snapshotId(),family,key);
        JsonNode rows=new ObjectMapper().readTree(new String(json,StandardCharsets.UTF_8))
            .path("categories").path("MEMBER_GROUPS");
        Arrays.fill(json,(byte)0);
        assertEquals(2,rows.size());

        tx.execute(status->{
            // Foreign keys are intentionally enforced inside a staging schema.
            // The deliberate invalid insert proves failure/rollback isolation.
            db.execute("CREATE TEMP TABLE fp_restore_groups(id bigint PRIMARY KEY, name text NOT NULL) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_restore_links(parent_id bigint REFERENCES fp_restore_groups(id), child_id bigint REFERENCES fp_restore_groups(id)) ON COMMIT DROP");
            for(JsonNode row:rows) {
                assertEquals(family,row.path("family_id").asLong());
                db.update("INSERT INTO fp_restore_groups(id,name) VALUES (?,?)",
                    row.path("id").asLong(),row.path("name").asText());
            }
            assertEquals(2L,db.queryForObject("SELECT count(*) FROM fp_restore_groups",Long.class));
            db.update("INSERT INTO fp_restore_links(parent_id,child_id) VALUES (?,?)",parent,child);
            assertEquals(1L,db.queryForObject("SELECT count(*) FROM fp_restore_links",Long.class));
            // The links table enforces both FKs; a missing group cannot be
            // committed to this staging relation.
            assertEquals(2L,db.queryForObject("""
                    SELECT count(*) FROM pg_constraint
                    WHERE conrelid='pg_temp.fp_restore_links'::regclass AND contype='f'
                    """,Long.class));
            status.setRollbackOnly();
            return null;
        });
        assertEquals(2L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,family));
        assertEquals("Untouched neighbor",db.queryForObject(
            "SELECT name FROM member_groups WHERE family_id=?",String.class,other));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,family));
    }
    @Test void archivedGroupAndMediaBytesRoundtripThroughSqlWithRollbackAndTenantFence() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long source=db.queryForObject("INSERT INTO families(name) VALUES ('Roundtrip source') RETURNING id",Long.class);
        long neighbor=db.queryForObject("INSERT INTO families(name) VALUES ('Roundtrip neighbor') RETURNING id",Long.class);
        long group=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Archived group',true,false) RETURNING id",Long.class,source);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Other group',true,false)",neighbor);
        long role=db.queryForObject("INSERT INTO family_roles(family_id,code,name,system_role) VALUES (?,'PARENT','Parent',true) RETURNING id",Long.class,source);
        long member=db.queryForObject("INSERT INTO family_members(family_id,family_role_id,name,active,member_type) VALUES (?,?,'Media owner',true,'PARENT') RETURNING id",Long.class,source,role);
        UUID imageId=UUID.randomUUID();
        byte[] original=new byte[]{1,2,3,4,5,6,7,8};
        String hash=ProtectiveBackupArtifactCodec.sha(original);
        db.update("INSERT INTO media_assets(id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id) VALUES (?,?,'image/png',1,1,?,?,?)",
                imageId,source,original.length,hash,member);
        db.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?,'COVER',?)",imageId,original);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id) VALUES (?,'MEMBER_GROUP',?,'COVER',?)",source,group,imageId);
        var guard=new WorkspaceEpochTransactionGuard(db);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,guard),codec,portable);
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);var key=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        byte[] archive=tx.execute(status->exporter.export(source,UUID.randomUUID(),UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS","MEDIA_ASSETS","MEDIA_BINDINGS",
                        "TASK_DEFINITIONS","REWARD_DEFINITIONS","POINT_LEDGER",
                        "REPUTATION_LEDGER","TASK_EXECUTIONS"),"V50",key)).encryptedFile();
        // Exercise the reusable rollback-only engine against the same genuine
        // archive, rather than only recreating temporary tables in this test.
        var engine=new WorkspaceDisposableRestoreRehearsal(db,
                new DataSourceTransactionManager(ds),portable,codec,inspector);
        var report=engine.rehearse(archive,source,key);
        assertTrue(report.rolledBack());
        assertEquals(1,report.groups());
        assertEquals(1,report.assets());
        assertEquals(1,report.bindings());
        assertEquals(1,report.checkedVariants());
        assertEquals(0,inspector.inspect(archive,source,key).counts().get("TASK_DEFINITIONS"));
        assertEquals(0,inspector.inspect(archive,source,key).counts().get("POINT_LEDGER"));
        assertTrue(inspector.inspect(archive,source,key).availableCategories().contains("REPUTATION_LEDGER"));
        assertThrows(SecurityException.class,()->engine.rehearse(archive,neighbor,key));
        // Replaying the same portable file cannot leave a persistent staging
        // table or change the source family. Each rehearsal is rollback-only.
        var repeated=engine.rehearse(archive,source,key);
        assertEquals(report,repeated);
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,source));
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM media_assets WHERE workspace_id=?",Long.class,source));
        // Corruption must fail before touching even temporary PostgreSQL rows.
        byte[] corrupt=archive.clone();
        corrupt[corrupt.length-1]^=1;
        assertThrows(RuntimeException.class,()->engine.rehearse(corrupt,source,key));
        var checked=inspector.inspect(archive,source,key);
        assertFalse(checked.restorable());
        assertEquals(1,checked.counts().get("MEMBER_GROUPS"));
        assertEquals(1,checked.counts().get("MEDIA_ASSETS"));
        assertEquals(1,checked.counts().get("MEDIA_BINDINGS"));
        assertThrows(SecurityException.class,()->inspector.inspect(archive,neighbor,key));

        var envelope=portable.decode(archive);
        byte[] plaintext=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
        JsonNode categories=new ObjectMapper().readTree(plaintext).path("categories");
        Arrays.fill(plaintext,(byte)0);
        var archivedGroup=categories.path("MEMBER_GROUPS").get(0);
        var archivedAsset=categories.path("MEDIA_ASSETS").get(0);
        var archivedBinding=categories.path("MEDIA_BINDINGS").get(0);
        byte[] decoded=Base64.getDecoder().decode(archivedAsset.path("blobs").get(0).path("bytesBase64").asText());
        assertArrayEquals(original,decoded);
        assertEquals(hash,ProtectiveBackupArtifactCodec.sha(decoded));

        tx.execute(status->{
            db.execute("CREATE TEMP TABLE fp_stage_groups(id bigint PRIMARY KEY, family_id bigint NOT NULL, name text NOT NULL) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_stage_assets(id uuid PRIMARY KEY, workspace_id bigint NOT NULL, sha256 text NOT NULL, bytes bytea NOT NULL) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_stage_bindings(group_id bigint REFERENCES fp_stage_groups(id), asset_id uuid REFERENCES fp_stage_assets(id)) ON COMMIT DROP");
            db.update("INSERT INTO fp_stage_groups(id,family_id,name) VALUES (?,?,?)",
                    archivedGroup.path("id").asLong(),archivedGroup.path("family_id").asLong(),
                    archivedGroup.path("name").asText());
            db.update("INSERT INTO fp_stage_assets(id,workspace_id,sha256,bytes) VALUES (?,?,?,?)",
                    UUID.fromString(archivedAsset.path("id").asText()),
                    archivedAsset.path("workspace_id").asLong(),archivedAsset.path("sha256").asText(),decoded);
            db.update("INSERT INTO fp_stage_bindings(group_id,asset_id) VALUES (?,?)",
                    archivedBinding.path("target_id").asLong(),
                    UUID.fromString(archivedBinding.path("asset_id").asText()));
            assertEquals(1L,db.queryForObject("SELECT count(*) FROM fp_stage_bindings",Long.class));
            assertEquals("Archived group",db.queryForObject("SELECT name FROM fp_stage_groups WHERE id=?",
                    String.class,group));
            byte[] staged=db.queryForObject("SELECT bytes FROM fp_stage_assets WHERE id=?",
                    byte[].class,imageId);
            assertArrayEquals(original,staged);
            assertEquals(hash,ProtectiveBackupArtifactCodec.sha(staged));
            status.setRollbackOnly();
            return null;
        });
        assertEquals("Archived group",db.queryForObject("SELECT name FROM member_groups WHERE id=?",String.class,group));
        assertEquals("Other group",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,neighbor));
        assertArrayEquals(original,db.queryForObject("SELECT bytes FROM media_blobs WHERE asset_id=?",byte[].class,imageId));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,source));
        // Fault injection: a partial staging reconstruction with an invalid
        // media reference must abort atomically, not leave staged data behind.
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->tx.execute(status->{
            db.execute("CREATE TEMP TABLE fp_fault_group(id bigint PRIMARY KEY) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_fault_media(id uuid PRIMARY KEY) ON COMMIT DROP");
            db.execute("CREATE TEMP TABLE fp_fault_link(group_id bigint REFERENCES fp_fault_group(id), asset_id uuid REFERENCES fp_fault_media(id)) ON COMMIT DROP");
            db.update("INSERT INTO fp_fault_group(id) VALUES (?)",group);
            db.update("INSERT INTO fp_fault_link(group_id,asset_id) VALUES (?,?)",
                    group,UUID.randomUUID());
            return null;
        }));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM media_blobs WHERE asset_id=?",
                Long.class,imageId));

        // Source foreign keys and preserved archive identity must remain exact.
        assertEquals(1L,db.queryForObject("""
                SELECT count(*) FROM media_bindings
                WHERE workspace_id=? AND target_type='MEMBER_GROUP'
                  AND target_id=? AND asset_id=?
                """,Long.class,source,group,imageId));
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM media_assets WHERE workspace_id=?",
                Long.class,source));
        assertEquals(0L,db.queryForObject("SELECT count(*) FROM media_assets WHERE workspace_id=?",
                Long.class,neighbor));

    }

}
