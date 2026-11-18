package com.olehkobylianskyi.familypoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.service.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B-A-03c: a second database connection changes family-owned rows after the
 * source transaction has taken its first snapshot. The encrypted archive must
 * still represent exactly the original coherent MVCC snapshot.
 *
 * Tests the actual PostgreSQL isolation level; it does not grant Restore.
 */
@Testcontainers
class WorkspacePortableBackupConcurrentSnapshotPostgresTest {
    @Container static final PostgreSQLContainer<?> DB =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("snapshot_consistency")
                    .withUsername("snapshot_test")
                    .withPassword("snapshot_test");

    @Test
    void concurrentUnfencedWriterCannotProduceMixedTimePortableArchive() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var jdbc=new JdbcTemplate(ds);
        long family=jdbc.queryForObject(
                "INSERT INTO families(name) VALUES ('B-A-03c snapshot family') RETURNING id", Long.class);
        long neighbor=jdbc.queryForObject(
                "INSERT INTO families(name) VALUES ('B-A-03c neighbor') RETURNING id", Long.class);
        long group=jdbc.queryForObject(
                "INSERT INTO member_groups(family_id,name,active,show_in_navigation) " +
                "VALUES (?,'Before',true,false) RETURNING id",Long.class,family);
        jdbc.update("INSERT INTO member_groups(family_id,name,active,show_in_navigation) " +
                "VALUES (?,'Neighbor',true,false)",neighbor);

        var guard=new WorkspaceEpochTransactionGuard(jdbc);
        var snapshots=new WorkspaceResetSnapshotService(jdbc,guard);
        var codec=new ProtectiveBackupArtifactCodec();
        var files=new ProtectiveBackupPortableFile();
        var exporter=new WorkspacePortableBackupExporter(jdbc,snapshots,codec,files);
        var keyGen=KeyGenerator.getInstance("AES");
        keyGen.init(256);
        var key=keyGen.generateKey();

        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var pool=Executors.newSingleThreadExecutor();
        try {
            var archive=tx.execute(status->{
                assertEquals("repeatable read",
                        jdbc.queryForObject("SHOW transaction_isolation",String.class));
                // First digest acquires the family fence and pins this transaction's
                // MVCC snapshot. The other writer deliberately bypasses the fence.
                String first=snapshots.digest(family,List.of("MEMBER_GROUPS"));
                var writer=pool.submit(()->{
                    var separateJdbc=new JdbcTemplate(new DriverManagerDataSource(
                            DB.getJdbcUrl(),DB.getUsername(),DB.getPassword()));
                    assertEquals(1,separateJdbc.update(
                            "UPDATE member_groups SET name='After' WHERE id=?",group));
                });
                try {writer.get(15,TimeUnit.SECONDS);}
                catch(Exception e){throw new IllegalStateException("Concurrent writer failed",e);}
                assertEquals("Before",jdbc.queryForObject(
                        "SELECT name FROM member_groups WHERE id=?",String.class,group));
                var result=exporter.export(family,UUID.randomUUID(),UUID.randomUUID(),
                        0L,List.of("MEMBER_GROUPS"),"V50",key);
                assertEquals(first,result.contentDigest());
                assertEquals(1,result.recordCounts().get("MEMBER_GROUPS"));
                return result;
            });
            assertNotNull(archive);
            var artifact=files.decode(archive.encryptedFile());
            var plain=codec.open(artifact,artifact.manifest().snapshotId(),family,key);
            var rows=new ObjectMapper().readTree(new String(plain,StandardCharsets.UTF_8))
                    .path("categories").path("MEMBER_GROUPS");
            assertEquals(1,rows.size());
            assertEquals("Before",rows.get(0).path("name").asText(),
                    "Archive must not mix the committed newer row with old digest");
            // The writer really committed; this was not a blocked/no-op race.
            assertEquals("After",jdbc.queryForObject(
                    "SELECT name FROM member_groups WHERE id=?",String.class,group));
            assertEquals("Neighbor",jdbc.queryForObject(
                    "SELECT name FROM member_groups WHERE family_id=?",String.class,neighbor));
            assertFalse(new ProtectiveBackupImportInspector(files,codec)
                    .inspect(archive.encryptedFile(),family,key).restorable());
        } finally {
            pool.shutdownNow();
        }
    }
}
