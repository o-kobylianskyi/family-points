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
import org.testcontainers.postgresql.PostgreSQLContainer;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/**
 * IMP-01a real DB→FPB1 AES-GCM archive→offline inspection AFTER PG18 loss.
 * All source rows disposable. No live workspaces, HTTP or production restore.
 */
class WorkspacePortableAncestryV2SourceLossPostgresTest {
    @Test void sealedRosterSurvivesSourceLossAndDeniesTamperedOrCrossTenantArchive() throws Exception {
        var container=new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("portable_v2_source")
                .withUsername("qa").withPassword("qa");
        byte[] stored=null;
        long originalWorkspace=0,otherWorkspace=0;
        UUID snapshot=UUID.randomUUID();
        var key=new SecretKeySpec(new byte[32],"AES");
        var codec=new ProtectiveBackupArtifactCodec();
        var portable=new ProtectiveBackupPortableFile();
        var offline=new ProtectiveBackupImportInspector(portable,codec);
        try {
            container.start();
            var ds=new DriverManagerDataSource(container.getJdbcUrl(),
                    container.getUsername(),container.getPassword());
            Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                    .load().migrate();
            var db=new JdbcTemplate(ds);
            long family=db.queryForObject(
                    "INSERT INTO families(name) VALUES (?) RETURNING id",
                    Long.class,"V2 source family");
            long neighbor=db.queryForObject(
                    "INSERT INTO families(name) VALUES (?) RETURNING id",
                    Long.class,"Other QA family");
            originalWorkspace=family;
            otherWorkspace=neighbor;
            long role=db.queryForObject("""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,Long.class,family);
            long neighborRole=db.queryForObject("""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,Long.class,neighbor);
            long active=db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Never export this private name',true) RETURNING id
                    """,Long.class,family,role);
            long tombstone=db.queryForObject("""
                    INSERT INTO family_members(
                        family_id,family_role_id,member_type,name,active,deleted_at)
                    VALUES (?,?,'CHILD','Deleted private name',false,
                        TIMESTAMP '2026-11-01 08:09:10') RETURNING id
                    """,Long.class,family,role);
            db.update("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Neighbor protected',true)
                    """,neighbor,neighborRole);
            var exporter=new WorkspacePortableAncestryV2Exporter(db,codec,portable);
            var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
            tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            assertThrows(IllegalStateException.class,()->
                    exporter.export(family,snapshot,UUID.randomUUID(),0,key));
            var result=tx.execute(s->exporter.export(
                    family,snapshot,UUID.randomUUID(),0,key));
            assertEquals(1,result.roleCount());
            assertEquals(2,result.memberCount());
            stored=result.encryptedFile();
            var envelope=portable.decode(stored);
            byte[] plain=codec.open(envelope,snapshot,family,key);
            try {
                var json=new ObjectMapper().readTree(plain);
                assertEquals("family-points-export-v2",json.path("format").asText());
                assertEquals(2,json.path("categories").path("FAMILY_MEMBERS").size());
                var text=new String(plain,StandardCharsets.UTF_8);
                for(String sensitive:List.of("Never export this private name",
                        "Deleted private name","Neighbor protected",
                        "password_hash","provider_subject","email"))
                    assertFalse(text.contains(sensitive),sensitive);
                assertTrue(text.contains(Long.toString(active)));
                assertTrue(text.contains(Long.toString(tombstone)));
                assertTrue(text.contains("2026-11-01T08:09:10"));
                assertEquals(result.manifestRootDigest(),
                        json.path("manifestRootDigest").asText());
            }finally{Arrays.fill(plain,(byte)0);}
        }finally{
            container.stop(); // Original source database definitively unavailable.
        }
        // No DB connection or JdbcTemplate below: archive must stand alone.
        assertNotNull(stored);
        final byte[] file=stored;
        final long source=originalWorkspace;
        final long other=otherWorkspace;
        var proof=offline.inspectAncestryV2(file,source,key);
        assertEquals(snapshot,proof.snapshotId());
        assertEquals(1,proof.roleCount());
        assertEquals(2,proof.memberCount());
        assertEquals(List.of(),proof.sourceRoleIds().stream().filter(x->x<=0).toList());
        assertEquals(1,proof.sourceRoleIds().size());
        assertEquals(2,proof.sourceMemberIds().size());
        assertTrue(proof.sourceMemberIds().get(0)<proof.sourceMemberIds().get(1));
        assertThrows(UnsupportedOperationException.class,
                ()->proof.sourceMemberIds().add(-1L));
        assertFalse(proof.restorable());
        assertThrows(SecurityException.class,()->
                offline.inspectAncestryV2(file,other,key));
        var corrupted=file.clone();
        corrupted[corrupted.length-1]^=1;
        assertThrows(SecurityException.class,()->
                offline.inspectAncestryV2(corrupted,source,key));
        var different=new byte[32];different[0]=1;
        var otherKey=new SecretKeySpec(different,"AES");
        assertThrows(SecurityException.class,()->
                offline.inspectAncestryV2(file,source,otherKey));
        // A reader possessing the encryption key can re-seal modified JSON,
        // but cannot make a stale manifestRootDigest pass the independent
        // category/root proof. Explicitly protect against trusting JSON root.
        var old=portable.decode(file);
        byte[] original=codec.open(old,snapshot,source,key);
        try {
            var modified=(com.fasterxml.jackson.databind.node.ObjectNode)
                    new ObjectMapper().readTree(original);
            var rows=(com.fasterxml.jackson.databind.node.ArrayNode)
                    modified.path("categories").path("FAMILY_MEMBERS");
            ((com.fasterxml.jackson.databind.node.ObjectNode)rows.get(0))
                    .put("activeAtSnapshot",false);
            var edited=new ObjectMapper().writeValueAsBytes(modified);
            try {
                var sealed=codec.seal(snapshot,source,old.manifest().operationId(),
                        old.manifest().dataEpoch(),old.manifest().sourceDigest(),
                        PortableManifestRootV2.SCHEMA,edited,key);
                assertThrows(SecurityException.class,()->
                        offline.inspectAncestryV2(portable.encode(sealed),source,key));
            }finally{Arrays.fill(edited,(byte)0);}
        }finally{Arrays.fill(original,(byte)0);}
    }
}
