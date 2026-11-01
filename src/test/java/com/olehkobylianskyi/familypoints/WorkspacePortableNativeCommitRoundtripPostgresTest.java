package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Actual native INSERT + COMMIT only inside a disposable Testcontainers database.
 * This is intentionally test-scoped: no production controller or service can invoke it.
 */
@Testcontainers
class WorkspacePortableNativeCommitRoundtripPostgresTest {
    @Container static final PostgreSQLContainer DB=new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("native_commit_restore").withUsername("tester").withPassword("tester_password");

    @Test void committedSelectiveRestoreRemapsGroupsAndCatalogBindingsAndPreservesNeighbors() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long source=db.queryForObject("INSERT INTO families(name) VALUES ('Archive source') RETURNING id",Long.class);
        long target=db.queryForObject("INSERT INTO families(name) VALUES ('Disposable restore target') RETURNING id",Long.class);
        long neighbor=db.queryForObject("INSERT INTO families(name) VALUES ('Unrelated family') RETURNING id",Long.class);
        long groupA=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'One',true,true) RETURNING id",Long.class,source);
        long groupB=db.queryForObject("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Two',true,false) RETURNING id",Long.class,source);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key) VALUES (?,'MEMBER_GROUP',?,'ICON','catalog-one')",source,groupA);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key) VALUES (?,'MEMBER_GROUP',?,'COVER','catalog-two')",source,groupB);
        db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Untouched',true,false)",neighbor);
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(portable,codec);
        var exporter=new WorkspacePortableBackupExporter(db,new WorkspaceResetSnapshotService(db,
                new WorkspaceEpochTransactionGuard(db)),codec,portable);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        byte[] file=tx.execute(status->exporter.export(source,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEMBER_GROUPS","MEDIA_BINDINGS"),"V50",key)).encryptedFile();
        var inspected=inspector.inspect(file,source,key);
        assertEquals(2,inspected.counts().get("MEMBER_GROUPS"));
        assertEquals(2,inspected.counts().get("MEDIA_BINDINGS"));
        var envelope=portable.decode(file);
        byte[] plaintext=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
        JsonNode cats;
        try {cats=new ObjectMapper().readTree(plaintext).path("categories");}
        finally {Arrays.fill(plaintext,(byte)0);}

        // Run the reusable test-only executor against a separate disposable
        // target, committing native SQL and testing repeated apply rejection.
        long reusableTarget=db.queryForObject(
                "INSERT INTO families(name) VALUES ('Executor target') RETURNING id",Long.class);
        var executor=new DisposableNativeRestoreExecutor(db,
                new DataSourceTransactionManager(ds),portable,codec,inspector);
        var first=executor.apply(file,source,reusableTarget,key);
        assertEquals(2,first.groupIds().size());
        assertEquals(2,first.bindings());
        assertEquals(2L,db.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,reusableTarget));
        assertEquals(2L,db.queryForObject(
                "SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,reusableTarget));
        assertThrows(IllegalStateException.class,
                ()->executor.apply(file,source,reusableTarget,key));
        assertThrows(SecurityException.class,
                ()->executor.apply(file,neighbor,target,key));
        byte[] corrupt=file.clone();
        corrupt[corrupt.length-1]^=1;
        assertThrows(RuntimeException.class,
                ()->executor.apply(corrupt,source,target,key));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,target));
        long staleTarget=db.queryForObject(
                "INSERT INTO families(name,data_epoch) VALUES ('Stale disposable target',1) RETURNING id",
                Long.class);
        assertThrows(IllegalStateException.class,
                ()->executor.apply(file,source,staleTarget,key));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,staleTarget));


        // No rollback here: genuine SQL commit in disposable database only.
        var idMap=tx.execute(status->{
            var map=new TreeMap<Long,Long>();
            assertEquals(0L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,target));
            for(JsonNode row:cats.path("MEMBER_GROUPS")) {
                assertEquals(source,row.path("family_id").asLong());
                long newId=db.queryForObject("""
                        INSERT INTO member_groups(family_id,name,active,show_in_navigation)
                        VALUES (?,?,?,?) RETURNING id
                        """,Long.class,target,row.path("name").asText(),
                        row.path("active").asBoolean(),row.path("show_in_navigation").asBoolean());
                map.put(row.path("id").asLong(),newId);
            }
            for(JsonNode binding:cats.path("MEDIA_BINDINGS")) {
                assertEquals(source,binding.path("workspace_id").asLong());
                assertEquals("MEMBER_GROUP",binding.path("target_type").asText());
                assertFalse(binding.hasNonNull("asset_id"));
                long newGroup=Objects.requireNonNull(map.get(binding.path("target_id").asLong()));
                db.update("""
                        INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key)
                        VALUES (?,'MEMBER_GROUP',?,?,?)
                        """,target,newGroup,binding.path("slot").asText(),binding.path("catalog_key").asText());
            }
            assertEquals(2L,db.queryForObject("SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,target));
            return Map.copyOf(map);
        });
        assertEquals(2,idMap.size());
        assertNotEquals(groupA,idMap.get(groupA));
        assertNotEquals(groupB,idMap.get(groupB));
        assertEquals(List.of("One","Two"),db.queryForList(
                "SELECT name FROM member_groups WHERE family_id=? ORDER BY name",String.class,target));
        assertEquals(List.of("catalog-one","catalog-two"),db.queryForList(
                "SELECT catalog_key FROM media_bindings WHERE workspace_id=? ORDER BY catalog_key",
                String.class,target));
        assertEquals(2L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,source));
        assertEquals(2L,db.queryForObject("SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,source));
        assertEquals("Untouched",db.queryForObject("SELECT name FROM member_groups WHERE family_id=?",String.class,neighbor));

        // A second import with an invalid FK is atomically rejected.
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->tx.execute(status->{
            db.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) VALUES (?,'Must roll back',true,false)",target);
            db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key) VALUES (?,'MEMBER_GROUP',?,'ICON','duplicate')",
                    target,idMap.get(groupA));
            return null;
        }));
        assertEquals(2L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,target));
        assertEquals(2L,db.queryForObject("SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,target));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,neighbor));
    }
}
