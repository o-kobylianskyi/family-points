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
        byte[] historyStored=null;
        long originalWorkspace=0,otherWorkspace=0;
        long originalActiveMember=0,originalDeletedMember=0;
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
            originalActiveMember=active;
            originalDeletedMember=tombstone;
            db.update("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Neighbor protected',true)
                    """,neighbor,neighborRole);
            // REAL migrated PostgreSQL history, not synthetic JSON: this
            // fixture exercises all six supported economic/task categories.
            // The neighboring family carries a separate currency and must
            // never leak into the archived source family.
            long pointType=db.queryForObject("""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'POINTS','Points',true,0) RETURNING id
                    """,Long.class,family);
            db.update("""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'NEIGHBOR_ONLY','Neighbor currency',true,0)
                    """,neighbor);
            long task=db.queryForObject("""
                    INSERT INTO task_definitions(
                        family_id,assigned_member_id,title,description,mandatory,
                        delegation_allowed,recurrence_type,active,created_at,updated_at,
                        reward_point_type_id,reward_amount)
                    VALUES (?,?,'PG18 historical task','Source-only QA',false,
                        false,'ONCE',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,5)
                    RETURNING id
                    """,Long.class,family,active,pointType);
            long reward=db.queryForObject("""
                    INSERT INTO reward_definitions(
                        family_id,title,description,point_type_id,price_amount,
                        cooldown_seconds,extra_request_allowed,requires_approval,
                        reward_kind,acquisition_mode,availability_mode,active,
                        created_at,updated_at)
                    VALUES (?,'PG18 historical reward','Source-only QA',?,4,
                        30,false,false,'STANDARD','DIRECT','ALWAYS',true,
                        CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    RETURNING id
                    """,Long.class,family,pointType);
            long execution=db.queryForObject("""
                    INSERT INTO task_instances(
                        task_definition_id,member_id,original_assigned_member_id,
                        scheduled_date,slot_key,title,description,mandatory,
                        reward_point_type_id,reward_amount,status,
                        reward_processed,penalty_processed)
                    VALUES (?,?,?,DATE '2026-11-12','default',
                        'PG18 historical task','Source-only QA',false,
                        ?,5,'COMPLETED',true,false)
                    RETURNING id
                    """,Long.class,task,active,active,pointType);
            long purchase=db.queryForObject("""
                    INSERT INTO reward_purchases(
                        reward_definition_id,member_id,reward_title,point_type_id,
                        price_amount,status,purchased_at)
                    VALUES (?,?,'PG18 historical reward',?,4,'READY',CURRENT_TIMESTAMP)
                    RETURNING id
                    """,Long.class,reward,active,pointType);
            db.update("""
                    INSERT INTO point_transactions(
                        member_id,point_type_id,source_type,amount,type,source_id,created_at)
                    VALUES (?,?,'TASK',5,'EARN',?,CURRENT_TIMESTAMP)
                    """,active,pointType,execution);
            db.update("""
                    INSERT INTO point_transactions(
                        member_id,point_type_id,source_type,amount,type,source_id,created_at)
                    VALUES (?,?,'REWARD_PURCHASE',-4,'SPEND',?,CURRENT_TIMESTAMP)
                    """,active,pointType,purchase);
            db.update("""
                    INSERT INTO point_transactions(
                        member_id,point_type_id,source_type,amount,type,created_at)
                    VALUES (?,?,'MANUAL',2,'ADJUSTMENT',CURRENT_TIMESTAMP)
                    """,active,pointType);
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
            // The real populated original-family task + finance graph is
            // sealed from one PG18 snapshot before the source DB is destroyed.
            var historical=tx.execute(status->exporter.exportWithHistory(
                    family,UUID.randomUUID(),UUID.randomUUID(),0,key));
            assertNotNull(historical);
            historyStored=historical.encryptedFile();
            // A signed bounded archive must never silently omit a live
            // reward request outside its six supported historical tables.
            long outside=db.queryForObject("""
                    INSERT INTO reward_requests(
                        family_id,requested_by_member_id,title,status)
                    VALUES (?,?,'Unsupported request','PENDING') RETURNING id
                    """,Long.class,family,active);
            assertThrows(SecurityException.class,()->tx.execute(status->
                    exporter.exportWithHistory(family,UUID.randomUUID(),
                            UUID.randomUUID(),0,key)));
            db.update("DELETE FROM reward_requests WHERE id=?",outside);
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
        final byte[] historyFile=historyStored;
        final long source=originalWorkspace;
        final long other=otherWorkspace;
        var proof=offline.inspectAncestryV2(file,source,key);
        // Includes 6 history categories in the existing v2 content/root,
        // without needing the original PostgreSQL service after DB loss.
        var historyProof=offline.inspectAncestryV2(historyFile,source,key);
        assertEquals(1,historyProof.roleCount());
        assertEquals(2,historyProof.memberCount());
        assertEquals(8,historyProof.verifiedCategoryCounts().size());
        assertEquals(Integer.valueOf(1),historyProof.verifiedCategoryCounts().get("POINT_TYPES"));
        assertEquals(Integer.valueOf(1),historyProof.verifiedCategoryCounts().get("TASK_DEFINITIONS"));
        assertEquals(Integer.valueOf(1),historyProof.verifiedCategoryCounts().get("REWARD_DEFINITIONS"));
        assertEquals(Integer.valueOf(1),historyProof.verifiedCategoryCounts().get("TASK_EXECUTIONS"));
        assertEquals(Integer.valueOf(1),historyProof.verifiedCategoryCounts().get("REWARD_PURCHASES"));
        assertEquals(Integer.valueOf(3),historyProof.verifiedCategoryCounts().get("POINT_LEDGER"));
        // After source DB loss, decrypted history remains scoped to one family.
        var sealedHistory=portable.decode(historyFile);
        byte[] authenticHistory=codec.open(sealedHistory,
                sealedHistory.manifest().snapshotId(),source,key);
        try {
            String payload=new String(authenticHistory,StandardCharsets.UTF_8);
            assertTrue(payload.contains("PG18 historical task"));
            assertTrue(payload.contains("PG18 historical reward"));
            assertFalse(payload.contains("NEIGHBOR_ONLY"));
            assertFalse(payload.contains("Neighbor currency"));
            assertFalse(payload.contains("Neighbor protected"));
            assertFalse(payload.contains("Never export this private name"));
        }finally{Arrays.fill(authenticHistory,(byte)0);}
        var cSource=historyProof.verifiedSourceArchiveV2();
        assertEquals(source,cSource.sourceWorkspaceId());
        assertEquals("family-points-export-v2",cSource.schemaVersion());
        assertEquals(historyProof.manifestRootDigest(),cSource.manifestRootDigest());
        assertEquals(historyProof.authenticatedSnapshotDigest(),cSource.snapshotDigest());
        // Even a malicious actor able to re-encrypt an altered history payload
        // cannot retain its original authenticated manifest/root provenance.
        byte[] historyPlain=codec.open(sealedHistory,
                sealedHistory.manifest().snapshotId(),source,key);
        try {
            var mutatedHistory=(com.fasterxml.jackson.databind.node.ObjectNode)
                    new ObjectMapper().readTree(historyPlain);
            var ledger=mutatedHistory.path("categories").path("POINT_LEDGER");
            ((com.fasterxml.jackson.databind.node.ObjectNode)ledger.get(0))
                    .put("source_id",999_999L);
            byte[] alteredHistory=new ObjectMapper().writeValueAsBytes(mutatedHistory);
            try {
                var resealed=codec.seal(sealedHistory.manifest().snapshotId(),source,
                        sealedHistory.manifest().operationId(),
                        sealedHistory.manifest().dataEpoch(),
                        sealedHistory.manifest().sourceDigest(),
                        PortableManifestRootV2.SCHEMA,alteredHistory,key);
                assertThrows(SecurityException.class,()->
                        offline.inspectAncestryV2(portable.encode(resealed),source,key));
            }finally{Arrays.fill(alteredHistory,(byte)0);}
        }finally{Arrays.fill(historyPlain,(byte)0);}
        assertThrows(UnsupportedOperationException.class,
                ()->historyProof.verifiedCategoryCounts().put("OTHER",99));
        assertFalse(historyProof.restorable());
        assertEquals(snapshot,proof.snapshotId());
        assertEquals(1,proof.roleCount());
        assertEquals(2,proof.memberCount());
        assertEquals(List.of(),proof.sourceRoleIds().stream().filter(x->x<=0).toList());
        assertEquals(1,proof.sourceRoleIds().size());
        assertEquals(2,proof.sourceMemberIds().size());
        assertTrue(proof.sourceMemberIds().get(0)<proof.sourceMemberIds().get(1));
        assertThrows(UnsupportedOperationException.class,
                ()->proof.sourceMemberIds().add(-1L));
        // This is the real offline source-roster → C canonical binary mapping
        // bridge. It is NOT an owner proof or a target member grant.
        var originalIds=proof.sourceMemberIds();
        var reviewed=Map.of(originalIds.get(0),1003L,originalIds.get(1),1004L);
        String cDigest=proof.canonicalMappingDigestForReviewedIds(other,reviewed);
        assertEquals(
                com.olehkobylianskyi.familypoints.security
                    .WorkspaceRestorePermitScopeV2.canonicalMappingDigest(
                            source,other,reviewed),cDigest);
        assertThrows(SecurityException.class,()->
                proof.canonicalMappingDigestForReviewedIds(
                        other,Map.of(originalIds.get(0),1003L)));
        assertThrows(IllegalArgumentException.class,()->
                proof.canonicalMappingDigestForReviewedIds(
                        other,Map.of(originalIds.get(0),1003L,originalIds.get(1),1003L)));
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
        nativeRestoreAfterSourceLoss(historyFile,source,key,offline,codec,portable,
                originalActiveMember,originalDeletedMember);
    }

    // B-INT-02: genuinely separate migrated PostgreSQL 18 target, AFTER source shutdown.
    // The real internal production services verify BCrypt, immutable original
    // creator enrollment, authenticated C scope, one-use permit and native FK
    // financial replay. No HTTP route or live workspace is involved.
    private static void nativeRestoreAfterSourceLoss(byte[] archive,long source,
            javax.crypto.SecretKey key,ProtectiveBackupImportInspector offline,
            ProtectiveBackupArtifactCodec codec,ProtectiveBackupPortableFile portable,
            long originalActive,long originalDeleted) throws Exception {
        try(var targetDb=new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("b_int_02_target").withUsername("qa").withPassword("qa")) {
            targetDb.start();
            var ds=new DriverManagerDataSource(targetDb.getJdbcUrl(),
                    targetDb.getUsername(),targetDb.getPassword());
            Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                    .load().migrate();
            var db=new JdbcTemplate(ds);
            var tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
            long target=db.queryForObject(
                    "INSERT INTO families(name) VALUES ('Restore target') RETURNING id",Long.class);
            long neighbor=db.queryForObject(
                    "INSERT INTO families(name) VALUES ('Restore neighbor') RETURNING id",Long.class);
            long role=db.queryForObject("""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,Long.class,target);
            long neighborRole=db.queryForObject("""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,Long.class,neighbor);
            long active=db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Restored target owner',true) RETURNING id
                    """,Long.class,target,role);
            long deleted=db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'CHILD','Mapped historic member',true) RETURNING id
                    """,Long.class,target,role);
            long foreign=db.queryForObject("""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Unrelated',true) RETURNING id
                    """,Long.class,neighbor,neighborRole);
            var encoder=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            long account=db.queryForObject("""
                    INSERT INTO user_accounts(username,password_hash,enabled,family_member_id)
                    VALUES (?,?,true,?) RETURNING id
                    """,Long.class,"restore_qa_"+UUID.randomUUID(),encoder.encode("SyntheticSecret42!"),active);
            db.update("""
                    INSERT INTO account_workspace_memberships(account_id,family_member_id,workspace_id)
                    VALUES (?,?,?)
                    """,account,active,target);
            var enrollment=new WorkspaceOriginalOwnerEnrollmentService(db);
            tx.execute(st->{enrollment.enrollNewCreator(target,account,active);return null;});
            var guard=new WorkspaceEpochTransactionGuard(db);
            var limiter=new WorkspaceRestorePasswordLimiter(db);
            var permits=new WorkspaceRestorePermitService(
                    db,guard,encoder,limiter,offline,portable,codec,
                    new GoogleTokenVerifier(""));
            var engine=new WorkspaceRestoreNativeTargetEngine(
                    db,guard,permits,portable,codec);
            var inspected=offline.inspectAncestryV2(archive,source,key);
            var approved=Map.of(originalActive,active,originalDeleted,deleted);
            var envelope=portable.decode(archive);
            byte[] plaintext=codec.open(envelope,envelope.manifest().snapshotId(),source,key);
            String selection;
            try {
                var root=(com.fasterxml.jackson.databind.node.ObjectNode)
                        new ObjectMapper().readTree(plaintext);
                selection=WorkspaceRestoreBoundedSelectionV2.fromVerifiedSource(
                        root,source,inspected.snapshotId(),inspected.sourceEpoch(),target,0);
            }finally{Arrays.fill(plaintext,(byte)0);}
            var now=java.time.Instant.now();
            var scope=new com.olehkobylianskyi.familypoints.security
                    .WorkspaceRestorePermitScopeV2.RestoreScope(
                    target,account,active,UUID.randomUUID(),
                    inspected.verifiedSourceArchiveV2(),selection,
                    inspected.canonicalMappingDigestForReviewedIds(target,approved),
                    0,now,now.plusSeconds(120));
            var wrong=Map.of(originalActive,active,originalDeleted,foreign);
            assertThrows(SecurityException.class,()->
                    tx.execute(st->{permits.issuePassword(scope,archive,key,wrong,
                            "SyntheticSecret42!");return null;}));
            tx.execute(st->{permits.issuePassword(scope,archive,key,approved,
                    "SyntheticSecret42!");return null;});
            // Inject one target-category conflict AFTER grant issuance. The
            // failed replay must roll back consumed_at and never bump epoch.
            long conflict=db.queryForObject("""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'CONFLICT','Existing',true,0) RETURNING id
                    """,Long.class,target);
            assertThrows(SecurityException.class,()->tx.execute(st->
                    engine.restoreBounded(scope,archive,key,approved)));
            assertNull(db.queryForObject("""
                    SELECT consumed_at FROM workspace_restore_permits WHERE operation_id=?
                    """,java.sql.Timestamp.class,scope.operationId()));
            assertEquals(0L,db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,target));
            db.update("DELETE FROM point_types WHERE id=?",conflict);
            var receipt=tx.execute(st->engine.restoreBounded(scope,archive,key,approved));
            assertNotNull(receipt);
            assertEquals(1,receipt.afterEpoch());
            assertEquals(3,receipt.insertedCounts().get("POINT_LEDGER"));
            assertEquals(3L,db.queryForObject("""
                    SELECT sum(t.amount) FROM point_transactions t
                    WHERE t.member_id=?
                    """,Long.class,active));
            assertEquals(1L,db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,target));
            assertEquals(1L,db.queryForObject(
                    "SELECT count(*) FROM workspace_restore_receipts WHERE operation_id=?",
                    Long.class,scope.operationId()));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    ()->db.update("DELETE FROM workspace_restore_receipts WHERE operation_id=?",
                            scope.operationId()));
            assertThrows(RuntimeException.class,()->tx.execute(st->
                    engine.restoreBounded(scope,archive,key,approved)));
            assertEquals(3L,db.queryForObject(
                    "SELECT count(*) FROM point_transactions t WHERE t.member_id=?",
                    Long.class,active));
            assertEquals(0L,db.queryForObject("""
                    SELECT count(*) FROM point_transactions t
                    JOIN family_members m ON m.id=t.member_id
                    WHERE m.family_id=?
                    """,Long.class,neighbor));
        }
    }
}
