package com.olehkobylianskyi.familypoints;

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
import java.util.List;
import java.util.UUID;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class WorkspacePortableMediaBindingPreflightPostgresTest {
    @Container static final PostgreSQLContainer DB =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("binding_preflight")
                    .withUsername("qa").withPassword("qa_password");

    @Test void rejectsMalformedBindingIdentityBeforeAnyDatabaseMutation() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        var db=new JdbcTemplate(ds);
        long family=db.queryForObject("INSERT INTO families(name) VALUES ('Binding source') RETURNING id",Long.class);
        long other=db.queryForObject("INSERT INTO families(name) VALUES ('Untouched neighbor') RETURNING id",Long.class);
        db.update("INSERT INTO media_bindings(workspace_id,target_type,target_id,slot,catalog_key) "
                +"VALUES (?,'WORKSPACE',?,'COVER','avatar-test')",family,family);
        var codec=new ProtectiveBackupArtifactCodec();
        var files=new ProtectiveBackupPortableFile();
        var inspector=new ProtectiveBackupImportInspector(files,codec);
        var exporter=new WorkspacePortableBackupExporter(db,
                new WorkspaceResetSnapshotService(db,new WorkspaceEpochTransactionGuard(db)),codec,files);
        var keyGen=KeyGenerator.getInstance("AES");
        keyGen.init(256);
        var key=keyGen.generateKey();
        var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        var backup=tx.execute(s->exporter.export(family,UUID.randomUUID(),UUID.randomUUID(),
                0L,List.of("MEDIA_BINDINGS"),"V50",key)).encryptedFile();
        assertEquals(1,inspector.inspect(backup,family,key).counts().get("MEDIA_BINDINGS"));
        var original=files.decode(backup);
        byte[] plain=codec.open(original,original.manifest().snapshotId(),family,key);
        try {
            var json=new ObjectMapper();
            for(int variant=0;variant<5;variant++) {
                var root=json.readTree(plain).deepCopy();
                var binding=((com.fasterxml.jackson.databind.node.ObjectNode)
                        root.path("categories").path("MEDIA_BINDINGS").get(0));
                switch(variant) {
                    case 0 -> binding.put("target_type","REWARD");
                    case 1 -> binding.put("target_id",-1);
                    case 2 -> binding.put("target_id",other);
                    case 3 -> binding.put("asset_id","not-a-uuid");
                    case 4 -> binding.put("slot","");
                    default -> throw new IllegalStateException();
                }
                byte[] invalid=json.writeValueAsBytes(root);
                try {
                    var sealed=codec.seal(UUID.randomUUID(),family,UUID.randomUUID(),0L,
                            original.manifest().sourceDigest(),"V50",invalid,key);
                    byte[] archive=files.encode(sealed);
                    assertThrows(IllegalArgumentException.class,
                            ()->inspector.inspect(archive,family,key),"variant "+variant);
                } finally {Arrays.fill(invalid,(byte)0);}
            }
            // Strict archive identifiers: decimal/string coercion must not
            // bypass exact composite key or tenant checks on an encrypted file.
            for(int variant=0;variant<4;variant++) {
                var root=json.readTree(plain).deepCopy();
                var binding=((com.fasterxml.jackson.databind.node.ObjectNode)
                        root.path("categories").path("MEDIA_BINDINGS").get(0));
                switch(variant) {
                    case 0 -> binding.put("target_id",family + 0.5);
                    case 1 -> binding.put("target_id",Long.toString(family));
                    case 2 -> binding.put("workspace_id",family + 0.5);
                    case 3 -> binding.put("workspace_id",Long.toString(family));
                    default -> throw new IllegalStateException();
                }
                byte[] malformed=json.writeValueAsBytes(root);
                try {
                    var sealed=codec.seal(UUID.randomUUID(),family,UUID.randomUUID(),0L,
                            original.manifest().sourceDigest(),"V50",malformed,key);
                    assertThrows(IllegalArgumentException.class,
                            ()->inspector.inspect(files.encode(sealed),family,key),
                            "nonintegral archive identifier variant "+variant);
                } finally {Arrays.fill(malformed,(byte)0);}
            }
            // Exporter maximum is 2000 rows per category. Imported archives
            // may be re-sealed by a valid key holder but may not exceed this limit.
            var overLimit=json.readTree(plain).deepCopy();
            var bindings=((com.fasterxml.jackson.databind.node.ArrayNode)
                    overLimit.path("categories").path("MEDIA_BINDINGS"));
            var originalBinding=bindings.get(0);
            for(int i=1;i<=2000;i++) {
                var row=((com.fasterxml.jackson.databind.node.ObjectNode)originalBinding.deepCopy());
                row.put("target_id",family+i);
                bindings.add(row);
            }
            byte[] excessive=json.writeValueAsBytes(overLimit);
            try {
                var sealed=codec.seal(UUID.randomUUID(),family,UUID.randomUUID(),0L,
                        original.manifest().sourceDigest(),"V50",excessive,key);
                assertThrows(IllegalArgumentException.class,
                        ()->inspector.inspect(files.encode(sealed),family,key));
            } finally {Arrays.fill(excessive,(byte)0);}
            // No physical FK exists for polymorphic media targets; missing
            // archive groups and unresolved member identity must be visible blockers.
            for(String targetType:List.of("MEMBER_GROUP","WORK_NODE","TASK","MEMBER")) {
                var root=json.readTree(plain).deepCopy();
                var binding=((com.fasterxml.jackson.databind.node.ObjectNode)
                        root.path("categories").path("MEDIA_BINDINGS").get(0));
                binding.put("target_type",targetType);
                binding.put("target_id",987654321);
                byte[] modified=json.writeValueAsBytes(root);
                try {
                    var sealed=codec.seal(UUID.randomUUID(),family,UUID.randomUUID(),0L,
                            original.manifest().sourceDigest(),"V50",modified,key);
                    var inspection=inspector.inspect(files.encode(sealed),family,key);
                    String blocker=switch(targetType) {
                        case "MEMBER_GROUP" -> "MISSING_CATEGORY:MEMBER_GROUPS";
                        case "WORK_NODE" -> "MISSING_CATEGORY:WORK_NODES";
                        case "TASK" -> "MISSING_CATEGORY:TASK_DEFINITIONS";
                        default -> "IDENTITY_REBIND_REQUIRED:MEDIA_BINDINGS.target_id";
                    };
                    assertTrue(inspection.blockers().contains(blocker),targetType);
                    assertFalse(inspection.restorable());
                } finally {Arrays.fill(modified,(byte)0);}
            }
            // Encrypted manifest can be genuine while its decrypted
            // category graph is malformed: duplicate asset keys, missing
            // referenced assets and noncanonical UUIDs must fail closed.
            var mediaId=UUID.randomUUID().toString();
            for(int variant=0;variant<3;variant++) {
                var root=json.readTree(plain).deepCopy();
                var categories=((com.fasterxml.jackson.databind.node.ObjectNode)root.path("categories"));
                var binding=((com.fasterxml.jackson.databind.node.ObjectNode)
                        categories.path("MEDIA_BINDINGS").get(0));
                binding.put("asset_id",mediaId);
                var assets=categories.putArray("MEDIA_ASSETS");
                var asset=assets.addObject();
                asset.put("id",mediaId);
                asset.put("workspace_id",family);
                asset.putArray("blobs");
                if(variant==0)assets.add(asset.deepCopy());
                if(variant==1)asset.put("id","invalid-asset-id");
                if(variant==2)asset.put("id",UUID.randomUUID().toString());
                byte[] modified=json.writeValueAsBytes(root);
                try {
                    var sealed=codec.seal(UUID.randomUUID(),family,UUID.randomUUID(),0L,
                            original.manifest().sourceDigest(),"V50",modified,key);
                    byte[] archive=files.encode(sealed);
                    if(variant<2)
                        assertThrows(IllegalArgumentException.class,
                                ()->inspector.inspect(archive,family,key),"asset variant "+variant);
                    else {
                        var inspection=inspector.inspect(archive,family,key);
                        assertTrue(inspection.blockers().contains(
                                "DANGLING_REFERENCE:MEDIA_BINDINGS.asset_id"));
                        assertFalse(inspection.restorable());
                    }
                } finally {Arrays.fill(modified,(byte)0);}
            }
        } finally {Arrays.fill(plain,(byte)0);}
        assertEquals(1L,db.queryForObject(
                "SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,family));
        assertEquals(0L,db.queryForObject(
                "SELECT count(*) FROM media_bindings WHERE workspace_id=?",Long.class,other));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,family));
        assertEquals(0L,db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,other));
    }
}
