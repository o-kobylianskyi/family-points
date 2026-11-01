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
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->{
                // Postgres moves a transaction to failed state after an FK
                // violation. A savepoint makes that negative check safe.
                var sp=new org.springframework.jdbc.datasource.ConnectionHolder(
                    org.springframework.jdbc.datasource.DataSourceUtils.getConnection(ds));
            });
            status.setRollbackOnly();
            return null;
        });
        assertEquals(2L,db.queryForObject("SELECT count(*) FROM member_groups WHERE family_id=?",Long.class,family));
        assertEquals("Untouched neighbor",db.queryForObject(
            "SELECT name FROM member_groups WHERE family_id=?",String.class,other));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,family));
    }
}
