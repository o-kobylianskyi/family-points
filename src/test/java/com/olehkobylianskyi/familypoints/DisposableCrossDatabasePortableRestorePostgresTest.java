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
 * Real physical DB boundary: archive from one disposable PostgreSQL container
 * is natively reconstructed in a SECOND disposable PostgreSQL container.
 * Test-only executor; no reset HTTP, verified promotion, or real-family data.
 */
@Testcontainers
class DisposableCrossDatabasePortableRestorePostgresTest {
    @Container static final PostgreSQLContainer SOURCE =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("fp_export_source")
                    .withUsername("source_test").withPassword("source_only");
    @Container static final PostgreSQLContainer TARGET =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("fp_restore_target")
                    .withUsername("target_test").withPassword("target_only");

    @Test void authenticatedArchiveRestoresGroupMediaAndPrivateTemplateAcrossIndependentDatabases()
            throws Exception {
        var sourceDs=new DriverManagerDataSource(
                SOURCE.getJdbcUrl(),SOURCE.getUsername(),SOURCE.getPassword());
        var targetDs=new DriverManagerDataSource(
                TARGET.getJdbcUrl(),TARGET.getUsername(),TARGET.getPassword());
        Flyway.configure().dataSource(sourceDs).locations("classpath:db/migration").load().migrate();
        Flyway.configure().dataSource(targetDs).locations("classpath:db/migration").load().migrate();
        var src=new JdbcTemplate(sourceDs);
        var dst=new JdbcTemplate(targetDs);

        long srcFamily=src.queryForObject(
                "INSERT INTO families(name) VALUES ('Export family') RETURNING id",Long.class);
        // Use a second source group so source and remapped target IDs are not
        // accidentally the same on two independent sequences.
        long oldGroup=src.queryForObject("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Archived group',true,false) RETURNING id
                """,Long.class,srcFamily);
        long srcRole=src.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,srcFamily);
        long srcMember=src.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Archive owner',true,'PARENT') RETURNING id
                """,Long.class,srcFamily,srcRole);
        byte[] mediaBytes=new byte[]{7,14,21,28,35,42};
        UUID originalAsset=UUID.randomUUID();
        src.update("""
                INSERT INTO media_assets
                  (id,workspace_id,mime_type,width,height,byte_size,sha256,created_by_member_id)
                VALUES (?,?,'image/png',1,1,?,?,?)
                """,originalAsset,srcFamily,mediaBytes.length,
                ProtectiveBackupArtifactCodec.sha(mediaBytes),srcMember);
        src.update("INSERT INTO media_blobs(asset_id,variant,bytes) VALUES (?,'COVER',?)",
                originalAsset,mediaBytes);
        src.update("""
                INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,asset_id)
                VALUES (?,'MEMBER_GROUP',?,'COVER',?)
                """,srcFamily,oldGroup,originalAsset);
        src.update("""
                INSERT INTO workspace_private_templates(workspace_id,kind,title,definition_json)
                VALUES (?,'TASK','Archived task template',?)
                """,srcFamily,"{\"kind\":\"TASK\",\"title\":\"Archived task template\"}");

        // Force the target workspace and ID sequences to differ from source.
        long neighbor=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Untouched neighbor') RETURNING id",
                Long.class);
        dst.update("""
                INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                VALUES (?,'Neighbor group',true,false)
                """,neighbor);
        long targetFamily=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Isolated restored family') RETURNING id",
                Long.class);
        assertNotEquals(srcFamily,targetFamily);
        long targetRole=dst.queryForObject("""
                INSERT INTO family_roles(family_id,code,name,system_role)
                VALUES (?,'PARENT','Parent',true) RETURNING id
                """,Long.class,targetFamily);
        long targetMember=dst.queryForObject("""
                INSERT INTO family_members(family_id,family_role_id,name,active,member_type)
                VALUES (?,?,'Mapped restore owner',true,'PARENT') RETURNING id
                """,Long.class,targetFamily,targetRole);

        var codec=new ProtectiveBackupArtifactCodec();
        var format=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(format,codec);
        var exporter=new WorkspacePortableBackupExporter(src,
                new WorkspaceResetSnapshotService(src,new WorkspaceEpochTransactionGuard(src)),
                codec,format);
        var keyGen=KeyGenerator.getInstance("AES");keyGen.init(256);
        var key=keyGen.generateKey();
        var sourceTx=new TransactionTemplate(new DataSourceTransactionManager(sourceDs));
        byte[] file=sourceTx.execute(s->exporter.export(srcFamily,UUID.randomUUID(),
                UUID.randomUUID(),0L,
                List.of("MEMBER_GROUPS","MEDIA_ASSETS","MEDIA_BINDINGS","PRIVATE_TEMPLATES"),
                "V50",key)).encryptedFile();

        var nativeRestore=new DisposableUnifiedGroupMediaRestoreExecutor(dst,
                new DataSourceTransactionManager(targetDs),format,codec,inspector);
        var outcome=nativeRestore.apply(file,srcFamily,targetFamily,
                Map.of(srcMember,targetMember),key);
        assertEquals(1,outcome.groupIds().size());
        assertEquals(1,outcome.assetIds().size());
        assertEquals(1,outcome.blobs());
        assertEquals(1,outcome.privateBindings());
        assertEquals(1,outcome.templateIds().size());
        long newGroup=outcome.groupIds().get(oldGroup);
        UUID newAsset=outcome.assetIds().get(originalAsset);
        assertNotEquals(oldGroup,newGroup);
        assertNotEquals(originalAsset,newAsset);
        assertEquals("Archived group",dst.queryForObject(
                "SELECT name FROM member_groups WHERE id=? AND family_id=?",
                String.class,newGroup,targetFamily));
        assertArrayEquals(mediaBytes,dst.queryForObject(
                "SELECT bytes FROM media_blobs WHERE asset_id=? AND variant='COVER'",
                byte[].class,newAsset));
        assertEquals(1L,dst.queryForObject("""
                SELECT count(*) FROM media_bindings
                WHERE workspace_id=? AND target_type='MEMBER_GROUP'
                  AND target_id=? AND asset_id=?
                """,Long.class,targetFamily,newGroup,newAsset));
        assertEquals("Archived task template",dst.queryForObject("""
                SELECT title FROM workspace_private_templates WHERE workspace_id=?
                """,String.class,targetFamily));
        assertEquals("Neighbor group",dst.queryForObject(
                "SELECT name FROM member_groups WHERE family_id=?",
                String.class,neighbor));
        assertEquals(0L,dst.queryForObject(
                "SELECT data_epoch FROM families WHERE id=?",Long.class,targetFamily));

        // Reapplying the same archive must fail, without duplicating rows.
        assertThrows(RuntimeException.class,()->nativeRestore.apply(
                file,srcFamily,targetFamily,Map.of(srcMember,targetMember),key));
        assertEquals(1L,dst.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,targetFamily));
        assertEquals(1L,dst.queryForObject(
                "SELECT count(*) FROM workspace_private_templates WHERE workspace_id=?",
                Long.class,targetFamily));

        // Integrity: a single byte changed in transit must fail closed.
        byte[] corrupted=file.clone();
        corrupted[corrupted.length-1]^=1;
        long corruptedTarget=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Corrupted archive target') RETURNING id",
                Long.class);
        assertThrows(RuntimeException.class,()->nativeRestore.apply(
                corrupted,srcFamily,corruptedTarget,Map.of(srcMember,targetMember),key));
        assertEquals(0L,dst.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,corruptedTarget));

        // Wrong AES key must fail authentication before any target mutations.
        var wrongKey=keyGen.generateKey();
        long reject=dst.queryForObject(
                "INSERT INTO families(name) VALUES ('Rejected target') RETURNING id",
                Long.class);
        assertThrows(RuntimeException.class,()->nativeRestore.apply(
                file,srcFamily,reject,Map.of(srcMember,targetMember),wrongKey));
        assertEquals(0L,dst.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",
                Long.class,reject));
        assertEquals(0L,dst.queryForObject(
                "SELECT count(*) FROM media_assets WHERE workspace_id=?",
                Long.class,reject));
    }
}
