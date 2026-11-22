package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.security.WorkspaceRestorePermitScopeV2;
import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.entity.WorkspaceMember;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * B-INT-02 actual native-schema disposable two-PG18 source-loss integration.
 * No application endpoint/real family is called; production enrollment,
 * password, permit and native writer implementations execute under PG TX.
 */
class WorkspaceNativeV2RestoreFinanceMatrixPostgresTest {
    private static long insert(JdbcTemplate db,String sql,Object... args) {
        Long id=db.queryForObject(sql,Long.class,args);
        assertNotNull(id);
        return id;
    }
    private static long insertedId(JdbcTemplate db,String sql,Object... args) {
        return insert(db,sql,args);
    }
    @Test void twoMembersTwoCurrenciesSourceLossExactLedgerParityAndNegatives() throws Exception {
        try(var source=new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("source_loss").withUsername("qa").withPassword("qa");
            var target=new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("restored_target").withUsername("qa").withPassword("qa")) {
            source.start();target.start();
            var srcDS=new DriverManagerDataSource(source.getJdbcUrl(),
                    source.getUsername(),source.getPassword());
            var dstDS=new DriverManagerDataSource(target.getJdbcUrl(),
                    target.getUsername(),target.getPassword());
            for(var ds:new DriverManagerDataSource[]{srcDS,dstDS})
                Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
            var a=new JdbcTemplate(srcDS);
            var b=new JdbcTemplate(dstDS);
            var srcTx=new TransactionTemplate(new DataSourceTransactionManager(srcDS));
            srcTx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            var dstTx=new TransactionTemplate(new DataSourceTransactionManager(dstDS));
            long sourceFamily=insert(a,"INSERT INTO families(name) VALUES ('Source') RETURNING id");
            long role=insert(a,"""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,sourceFamily);
            long sourceMember=insert(a,"""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Private source test name',true) RETURNING id
                    """,sourceFamily,role);
            long originalPoint=insert(a,"""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'POINTS','Points',true,0) RETURNING id
                    """,sourceFamily);
            long sourceTask=insert(a,"""
                    INSERT INTO task_definitions(
                       family_id,assigned_member_id,title,description,mandatory,
                       delegation_allowed,recurrence_type,active,created_at,updated_at,
                       reward_point_type_id,reward_amount)
                    VALUES (?,?,'Task','History',false,false,'ONCE',true,
                            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,5) RETURNING id
                    """,sourceFamily,sourceMember,originalPoint);
            long sourceReward=insert(a,"""
                    INSERT INTO reward_definitions(family_id,title,description,
                       point_type_id,price_amount,active,created_at,updated_at)
                    VALUES (?,'Reward','History',?,4,true,
                            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING id
                    """,sourceFamily,originalPoint);
            long execution=insert(a,"""
                    INSERT INTO task_instances(
                       task_definition_id,member_id,original_assigned_member_id,
                       scheduled_date,slot_key,title,description,mandatory,
                       reward_point_type_id,reward_amount,status,
                       reward_processed,penalty_processed)
                    VALUES (?,?,?,DATE '2026-11-12','default','Task','History',false,
                            ?,5,'COMPLETED',true,false) RETURNING id
                    """,sourceTask,sourceMember,sourceMember,originalPoint);
            long purchase=insert(a,"""
                    INSERT INTO reward_purchases(reward_definition_id,member_id,
                       reward_title,point_type_id,price_amount,status,purchased_at)
                    VALUES (?,?,'Reward',?,4,'READY',CURRENT_TIMESTAMP) RETURNING id
                    """,sourceReward,sourceMember,originalPoint);
            a.update("""
                    INSERT INTO point_transactions(member_id,point_type_id,
                        source_type,amount,type,source_id,created_at)
                    VALUES (?,?,'TASK',5,'EARN',?,CURRENT_TIMESTAMP)
                    """,sourceMember,originalPoint,execution);
            a.update("""
                    INSERT INTO point_transactions(member_id,point_type_id,
                        source_type,amount,type,source_id,created_at)
                    VALUES (?,?,'REWARD_PURCHASE',-4,'SPEND',?,CURRENT_TIMESTAMP)
                    """,sourceMember,originalPoint,purchase);
            a.update("""
                    INSERT INTO point_transactions(member_id,point_type_id,
                        source_type,amount,type,created_at)
                    VALUES (?,?,'MANUAL',2,'ADJUSTMENT',CURRENT_TIMESTAMP)
                    """,sourceMember,originalPoint);
            // A second source member/denomination with real task and reward
            // parents, plus a cross-product 2x2 ledger matrix.
            long secondMember=insert(a,"""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'CHILD','Source second',true) RETURNING id
                    """,sourceFamily,role);
            long secondPoint=insert(a,"""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'MINUTES','Minutes',true,1) RETURNING id
                    """,sourceFamily);
            long task2=insert(a,"""
                    INSERT INTO task_definitions(
                      family_id,assigned_member_id,title,description,mandatory,
                      delegation_allowed,recurrence_type,active,created_at,updated_at,
                      reward_point_type_id,reward_amount)
                    VALUES (?,?,'Second task','History',false,false,'ONCE',true,
                            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,9) RETURNING id
                    """,sourceFamily,secondMember,secondPoint);
            long reward2=insert(a,"""
                    INSERT INTO reward_definitions(family_id,title,description,
                      point_type_id,price_amount,active,created_at,updated_at)
                    VALUES (?,'Second reward','History',?,11,true,
                            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING id
                    """,sourceFamily,secondPoint);
            long execution2=insert(a,"""
                    INSERT INTO task_instances(
                      task_definition_id,member_id,original_assigned_member_id,
                      scheduled_date,slot_key,title,description,mandatory,
                      reward_point_type_id,reward_amount,status,
                      reward_processed,penalty_processed)
                    VALUES (?,?,?,DATE '2026-11-13','default','Second task','History',false,
                            ?,9,'COMPLETED',true,false) RETURNING id
                    """,task2,secondMember,secondMember,secondPoint);
            long purchase2=insert(a,"""
                    INSERT INTO reward_purchases(reward_definition_id,member_id,
                      reward_title,point_type_id,price_amount,status,purchased_at)
                    VALUES (?,?,'Second reward',?,11,'READY',CURRENT_TIMESTAMP) RETURNING id
                    """,reward2,secondMember,secondPoint);
            a.update("""
                    INSERT INTO point_transactions(member_id,point_type_id,
                      source_type,amount,type,source_id,created_at)
                    VALUES (?,?,'TASK',9,'EARN',?,CURRENT_TIMESTAMP)
                    """,secondMember,secondPoint,execution2);
            a.update("""
                    INSERT INTO point_transactions(member_id,point_type_id,
                      source_type,amount,type,source_id,created_at)
                    VALUES (?,?,'REWARD_PURCHASE',-11,'SPEND',?,CURRENT_TIMESTAMP)
                    """,secondMember,secondPoint,purchase2);
            a.update("""
                    INSERT INTO point_transactions(member_id,point_type_id,
                      source_type,amount,type,created_at)
                    VALUES (?,?,'MANUAL',11,'ADJUSTMENT',CURRENT_TIMESTAMP)
                    """,secondMember,originalPoint);
            a.update("""
                    INSERT INTO point_transactions(member_id,point_type_id,
                      source_type,amount,type,created_at)
                    VALUES (?,?,'MANUAL',7,'ADJUSTMENT',CURRENT_TIMESTAMP)
                    """,sourceMember,secondPoint);
            var codec=new ProtectiveBackupArtifactCodec();
            var portable=new ProtectiveBackupPortableFile();
            var inspector=new ProtectiveBackupImportInspector(portable,codec);
            var key=new SecretKeySpec(new byte[32],"AES");
            UUID snapshot=UUID.randomUUID();
            var exporter=new WorkspacePortableAncestryV2Exporter(a,codec,portable);
            byte[] sealed=srcTx.execute(tx->
                    exporter.exportWithHistory(sourceFamily,snapshot,UUID.randomUUID(),0,key)
                            .encryptedFile());
            assertNotNull(sealed);
            // The original SQL database is deliberately gone before consent.
            source.stop();
            var proof=inspector.inspectAncestryV2(sealed,sourceFamily,key);
            assertEquals(8,proof.verifiedCategoryCounts().size());
            assertFalse(proof.restorable());

            // Advance TARGET identity sequences using legitimate OTHER-family
            // rows. Source and target DB sequences are independent, so without
            // this padding matching numeric IDs could hide a broken remapper.
            long paddingFamily=insert(b,
                    "INSERT INTO families(name) VALUES ('Other-family ID padding') RETURNING id");
            long paddingRole=insert(b,"""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Padding') RETURNING id
                    """,paddingFamily);
            long paddingMember=insert(b,"""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Padding',true) RETURNING id
                    """,paddingFamily,paddingRole);
            for(int pad=0;pad<4;pad++) {
                insert(b,"""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'CHILD','Other-family row',true) RETURNING id
                    """,paddingFamily,paddingRole);
                insert(b,"""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,?,?,true,?) RETURNING id
                    """,paddingFamily,"PADDING_"+pad,"Other-family",pad);
            }
            long paddingPoint=insert(b,"""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'PADDING_ORIGINAL','Other-family',true,20) RETURNING id
                    """,paddingFamily);
            long paddingTask=insert(b,"""
                    INSERT INTO task_definitions(
                      family_id,assigned_member_id,title,description,mandatory,
                      delegation_allowed,recurrence_type,active,created_at,updated_at)
                    VALUES (?,?,'Other-family task','Padding',false,false,'ONCE',true,
                            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING id
                    """,paddingFamily,paddingMember);
            long paddingReward=insert(b,"""
                    INSERT INTO reward_definitions(family_id,title,description,
                      point_type_id,price_amount,active,created_at,updated_at)
                    VALUES (?,'Other-family reward','Padding',?,1,true,
                            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING id
                    """,paddingFamily,paddingPoint);
            insert(b,"""
                    INSERT INTO task_instances(
                      task_definition_id,member_id,original_assigned_member_id,
                      scheduled_date,slot_key,title,description,mandatory,status,
                      reward_processed,penalty_processed)
                    VALUES (?,?,?,DATE '2026-11-11','default',
                            'Other-family execution','Padding',false,'COMPLETED',true,false)
                    RETURNING id
                    """,paddingTask,paddingMember,paddingMember);
            insert(b,"""
                    INSERT INTO reward_purchases(reward_definition_id,member_id,
                      reward_title,point_type_id,price_amount,status,purchased_at)
                    VALUES (?,?,'Other-family purchase',?,1,'READY',CURRENT_TIMESTAMP)
                    RETURNING id
                    """,paddingReward,paddingMember,paddingPoint);

            long targetFamily=insert(b,"INSERT INTO families(name) VALUES ('Target') RETURNING id");
            long targetRole=insert(b,"""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Parent') RETURNING id
                    """,targetFamily);
            long targetMember=insert(b,"""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','QA target',true) RETURNING id
                    """,targetFamily,targetRole);
            long secondTargetMember=insert(b,"""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'CHILD','Target second',true) RETURNING id
                    """,targetFamily,targetRole);
            var encoder=new BCryptPasswordEncoder();
            String password="qa-secret-restore";
            long targetAccount=insert(b,"""
                    INSERT INTO user_accounts(username,password_hash,enabled,family_member_id)
                    VALUES (?,?,true,?) RETURNING id
                    ""","qa_restore_"+UUID.randomUUID(),encoder.encode(password),targetMember);
            b.update("""
                    INSERT INTO account_workspace_memberships(account_id,family_member_id,workspace_id)
                    VALUES (?,?,?)
                    """,targetAccount,targetMember,targetFamily);
            var registration=new WorkspaceOriginalOwnerEnrollmentService(b);
            dstTx.execute(tx->{
                WorkspaceNewFamilyTransactionEvidence.record(targetFamily,targetMember);
                registration.enrollNewCreator(targetFamily,targetAccount,targetMember);
                return null;
            });
            long targetPoint=insert(b,"""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'POINTS','Points',true,0) RETURNING id
                    """,targetFamily);
            long secondTargetPoint=insert(b,"""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'MINUTES','Minutes',true,1) RETURNING id
                    """,targetFamily);
            var memberMap=Map.of(sourceMember,targetMember,
                    secondMember,secondTargetMember);
            var currencyMap=Map.of(originalPoint,targetPoint,
                    secondPoint,secondTargetPoint);
            // Prove BOTH members and BOTH currencies really need nonidentity
            // remapping rather than coincidentally preserving source row IDs.
            assertNotEquals(sourceMember,targetMember);
            assertNotEquals(secondMember,secondTargetMember);
            assertNotEquals(originalPoint,targetPoint);
            assertNotEquals(secondPoint,secondTargetPoint);
            assertFalse(java.util.Set.of(sourceMember,secondMember)
                    .contains(targetMember));
            assertFalse(java.util.Set.of(sourceMember,secondMember)
                    .contains(secondTargetMember));
            assertFalse(java.util.Set.of(originalPoint,secondPoint)
                    .contains(targetPoint));
            assertFalse(java.util.Set.of(originalPoint,secondPoint)
                    .contains(secondTargetPoint));
            var sourceArchive=proof.verifiedSourceArchiveV2();
            String mapping=proof.canonicalMappingDigestForReviewedIds(targetFamily,memberMap);
            var now=java.time.Instant.now();
            var initial=new WorkspaceRestorePermitScopeV2.RestoreScope(
                    targetFamily,targetAccount,targetMember,UUID.randomUUID(),sourceArchive,
                    "0".repeat(64),mapping,0,now,now.plusSeconds(240));
            var selections=new WorkspaceRestoreCategorySelectionV2(portable,codec);
            String closure=selections.calculateCanonical(initial,sealed,key);
            var scope=new WorkspaceRestorePermitScopeV2.RestoreScope(
                    targetFamily,targetAccount,targetMember,initial.operationId(),sourceArchive,
                    closure,mapping,0,now,now.plusSeconds(240));
            var currentUser=mock(CurrentUserService.class);
            var user=mock(UserAccount.class);
            var member=mock(WorkspaceMember.class);
            var workspace=mock(Workspace.class);
            when(user.getId()).thenReturn(targetAccount);
            when(user.isEnabled()).thenReturn(true);
            when(member.getId()).thenReturn(targetMember);
            when(member.getWorkspace()).thenReturn(workspace);
            when(workspace.getId()).thenReturn(targetFamily);
            when(currentUser.getCurrentAccount()).thenReturn(user);
            when(currentUser.getCurrentMember()).thenReturn(member);
            var epoch=new WorkspaceEpochTransactionGuard(b);
            var limiter=new WorkspaceRestorePasswordLimiter(b);
            var permit=new WorkspaceRestorePermitService(
                    b,epoch,encoder,limiter,inspector,selections,currentUser,
                    portable,codec,new GoogleTokenVerifier(""));
            var nativeEngine=new WorkspaceNativeV2RestoreEngine(
                    b,epoch,permit,inspector,portable,codec);

            // Real native target negative checks, not just pure validator guards.
            var incorrectKey=new SecretKeySpec(new byte[]{1,1,1,1,1,1,1,1,
                    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},"AES");
            assertThrows(RuntimeException.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,sealed,incorrectKey,memberMap,currencyMap)));
            byte[] tampered=sealed.clone();
            tampered[tampered.length-1]^=0x01;
            assertThrows(RuntimeException.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,tampered,key,memberMap,currencyMap)));
            long otherFamily=insert(b,"INSERT INTO families(name) VALUES ('Foreign QA') RETURNING id");
            long otherRole=insert(b,"""
                    INSERT INTO family_roles(family_id,system_role,code,name)
                    VALUES (?,true,'PARENT','Other') RETURNING id
                    """,otherFamily);
            long foreignMember=insert(b,"""
                    INSERT INTO family_members(family_id,family_role_id,member_type,name,active)
                    VALUES (?,?,'PARENT','Other',true) RETURNING id
                    """,otherFamily,otherRole);
            assertThrows(RuntimeException.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,sealed,key,
                            Map.of(sourceMember,foreignMember,secondMember,secondTargetMember),currencyMap)));
            long wrongCurrency=insert(b,"""
                    INSERT INTO point_types(family_id,code,name,active,sort_order)
                    VALUES (?,'DIFFERENT','Different',true,1) RETURNING id
                    """,targetFamily);
            assertThrows(RuntimeException.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,sealed,key,memberMap,
                            Map.of(originalPoint,wrongCurrency,secondPoint,secondTargetPoint))));
            assertEquals(0L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_receipts WHERE target_workspace_id=?",
                    Long.class,targetFamily));

            assertThrows(SecurityException.class,()->dstTx.execute(tx->{
                permit.issuePassword(scope,sealed,key,memberMap,"wrong-password");
                return null;
            }));
            // Correct real bcrypt credential, immutable authenticated selection,
            // and verified original creator; no QA-only fake permit issuer.
            dstTx.execute(tx->{
                permit.issuePassword(scope,sealed,key,memberMap,password);
                return null;
            });
            assertEquals(1L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_permits WHERE operation_id=?",
                    Long.class,scope.operationId()));
            // Existing target business rows must fail without deleting or
            // consuming the signed one-use RESTORE grant.
            long conflicting=insert(b,"""
                    INSERT INTO task_definitions(
                      family_id,assigned_member_id,title,description,mandatory,
                      delegation_allowed,recurrence_type,active,created_at,updated_at)
                    VALUES (?,?,'Existing target','Do not overwrite',
                            false,false,'ONCE',true,
                            CURRENT_TIMESTAMP,CURRENT_TIMESTAMP) RETURNING id
                    """,targetFamily,targetMember);
            assertThrows(RuntimeException.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,sealed,key,memberMap,currencyMap)));
            assertEquals(1L,b.queryForObject(
                    "SELECT count(*) FROM task_definitions WHERE id=? AND family_id=?",
                    Long.class,conflicting,targetFamily));
            assertEquals(0L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_permits WHERE operation_id=? AND consumed_at IS NOT NULL",
                    Long.class,scope.operationId()));
            b.update("DELETE FROM task_definitions WHERE id=?",conflicting);

            // Source scope epoch=0 is immutable; changing target epoch must
            // reject even when the archive and permit are otherwise valid.
            b.update("UPDATE families SET data_epoch=1 WHERE id=?",targetFamily);
            assertThrows(RuntimeException.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,sealed,key,memberMap,currencyMap)));
            assertEquals(0L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_receipts WHERE target_workspace_id=?",
                    Long.class,targetFamily));
            b.update("UPDATE families SET data_epoch=0 WHERE id=?",targetFamily);

            // Simulate actual DB clock expiry without waiting five minutes.
            // The immutable caller scope is still current, but the persisted
            // permit has expired and must not be consumed.
            b.update("""
                    UPDATE workspace_restore_permits
                    SET issued_at=clock_timestamp()-interval '6 minutes',
                        expires_at=clock_timestamp()-interval '2 minutes'
                    WHERE operation_id=?
                    """,scope.operationId());
            assertThrows(RuntimeException.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,sealed,key,memberMap,currencyMap)));
            assertEquals(0L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_permits WHERE operation_id=? AND consumed_at IS NOT NULL",
                    Long.class,scope.operationId()));
            b.update("""
                    UPDATE workspace_restore_permits
                    SET issued_at=?,expires_at=? WHERE operation_id=?
                    """,java.sql.Timestamp.from(scope.issuedAt()),
                    java.sql.Timestamp.from(scope.expiresAt()),scope.operationId());

            // A deliberate failure after native apply MUST roll back all
            // inserts, permit consumption, epoch AND receipt together.
            assertThrows(IllegalStateException.class,()->dstTx.execute(tx->{
                nativeEngine.restore(scope,sealed,key,memberMap,currencyMap);
                throw new IllegalStateException("QA_INJECTED_POST_RESTORE_FAILURE");
            }));
            assertEquals(0L,b.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=?",Long.class,targetFamily));
            assertEquals(0L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_receipts WHERE operation_id=?",
                    Long.class,scope.operationId()));
            assertEquals(0L,b.queryForObject(
                    "SELECT count(*) FROM point_transactions t JOIN family_members m ON m.id=t.member_id WHERE m.family_id=?",
                    Long.class,targetFamily));
            assertEquals(0L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_permits WHERE operation_id=? AND consumed_at IS NOT NULL",
                    Long.class,scope.operationId()));
            // Two independent PG18 connections simultaneously attempt the
            // same operation: family epoch fence + FOR UPDATE permit allow
            // exactly ONE committed restore and receipt.
            var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
            java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(2);
            java.util.concurrent.CountDownLatch go=new java.util.concurrent.CountDownLatch(1);
            WorkspaceNativeV2RestoreEngine.Result result;
            try {
                java.util.concurrent.Callable<WorkspaceNativeV2RestoreEngine.Result> writer=()->{
                    ready.countDown();
                    if(!go.await(10,java.util.concurrent.TimeUnit.SECONDS))
                        throw new AssertionError("Writers not started together");
                    return dstTx.execute(tx->nativeEngine.restore(
                            scope,sealed,key,memberMap,currencyMap));
                };
                var first=executor.submit(writer);
                var second=executor.submit(writer);
                assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));
                go.countDown();
                var successes=new java.util.ArrayList<WorkspaceNativeV2RestoreEngine.Result>();
                int rejected=0;
                for(var future:java.util.List.of(first,second)) {
                    try {
                        successes.add(future.get(30,java.util.concurrent.TimeUnit.SECONDS));
                    } catch(java.util.concurrent.ExecutionException denied) {
                        assertTrue(denied.getCause() instanceof RuntimeException);
                        rejected++;
                    }
                }
                assertEquals(1,successes.size(),"Exactly one PG transaction may commit");
                assertEquals(1,rejected,"The duplicate writer must fail closed");
                result=successes.get(0);
            } finally {
                go.countDown();
                executor.shutdownNow();
            }
            assertNotNull(result);
            assertEquals(1,result.newEpoch());
            assertEquals(2,result.insertedCounts().get("TASK_EXECUTIONS"));
            assertEquals(7,result.insertedCounts().get("POINT_LEDGER"));
            assertEquals(3L,b.queryForObject("""
                    SELECT sum(t.amount) FROM point_transactions t
                    WHERE t.member_id=? AND t.point_type_id=?
                    """,Long.class,targetMember,targetPoint));
            assertEquals(7L,b.queryForObject("""
                    SELECT sum(t.amount) FROM point_transactions t
                    WHERE t.member_id=? AND t.point_type_id=?
                    """,Long.class,targetMember,secondTargetPoint));
            assertEquals(11L,b.queryForObject("""
                    SELECT sum(t.amount) FROM point_transactions t
                    WHERE t.member_id=? AND t.point_type_id=?
                    """,Long.class,secondTargetMember,targetPoint));
            assertEquals(-2L,b.queryForObject("""
                    SELECT sum(t.amount) FROM point_transactions t
                    WHERE t.member_id=? AND t.point_type_id=?
                    """,Long.class,secondTargetMember,secondTargetPoint));
            assertEquals(2L,b.queryForObject(
                    "SELECT count(*) FROM task_definitions WHERE family_id=?",
                    Long.class,targetFamily));
            assertEquals(2L,b.queryForObject(
                    "SELECT count(*) FROM reward_definitions WHERE family_id=?",
                    Long.class,targetFamily));
            // Verify native event FK *and* polymorphic ledger source_id
            // now point to remapped TARGET rows, never source numeric IDs.
            long restoredTask=insertedId(b,
                    "SELECT id FROM task_definitions WHERE family_id=? AND title='Task'",
                    targetFamily);
            long restoredTask2=insertedId(b,
                    "SELECT id FROM task_definitions WHERE family_id=? AND title='Second task'",
                    targetFamily);
            long restoredReward=insertedId(b,
                    "SELECT id FROM reward_definitions WHERE family_id=? AND title='Reward'",
                    targetFamily);
            long restoredReward2=insertedId(b,
                    "SELECT id FROM reward_definitions WHERE family_id=? AND title='Second reward'",
                    targetFamily);
            long restoredExecution=insertedId(b,
                    "SELECT id FROM task_instances WHERE task_definition_id=? AND member_id=?",
                    restoredTask,targetMember);
            long restoredExecution2=insertedId(b,
                    "SELECT id FROM task_instances WHERE task_definition_id=? AND member_id=?",
                    restoredTask2,secondTargetMember);
            long restoredPurchase=insertedId(b,
                    "SELECT id FROM reward_purchases WHERE reward_definition_id=? AND member_id=?",
                    restoredReward,targetMember);
            long restoredPurchase2=insertedId(b,
                    "SELECT id FROM reward_purchases WHERE reward_definition_id=? AND member_id=?",
                    restoredReward2,secondTargetMember);
            for(var pair:java.util.List.of(
                    new long[]{sourceTask,restoredTask},new long[]{task2,restoredTask2},
                    new long[]{sourceReward,restoredReward},new long[]{reward2,restoredReward2},
                    new long[]{execution,restoredExecution},new long[]{execution2,restoredExecution2},
                    new long[]{purchase,restoredPurchase},new long[]{purchase2,restoredPurchase2}))
                assertNotEquals(pair[0],pair[1],"Real parent/source IDs must be remapped");
            assertEquals(1L,b.queryForObject("""
                    SELECT count(*) FROM point_transactions
                    WHERE member_id=? AND point_type_id=?
                      AND source_type='TASK' AND source_id=? AND amount=5
                    """,Long.class,targetMember,targetPoint,restoredExecution));
            assertEquals(1L,b.queryForObject("""
                    SELECT count(*) FROM point_transactions
                    WHERE member_id=? AND point_type_id=?
                      AND source_type='TASK' AND source_id=? AND amount=9
                    """,Long.class,secondTargetMember,secondTargetPoint,restoredExecution2));
            assertEquals(1L,b.queryForObject("""
                    SELECT count(*) FROM point_transactions
                    WHERE member_id=? AND point_type_id=?
                      AND source_type='REWARD_PURCHASE' AND source_id=? AND amount=-4
                    """,Long.class,targetMember,targetPoint,restoredPurchase));
            assertEquals(1L,b.queryForObject("""
                    SELECT count(*) FROM point_transactions
                    WHERE member_id=? AND point_type_id=?
                      AND source_type='REWARD_PURCHASE' AND source_id=? AND amount=-11
                    """,Long.class,secondTargetMember,secondTargetPoint,restoredPurchase2));
            assertEquals(2L,b.queryForObject("""
                    SELECT count(*) FROM task_instances i
                    JOIN task_definitions d ON d.id=i.task_definition_id
                    JOIN family_members m ON m.id=i.member_id
                    WHERE d.family_id=? AND m.family_id=?
                    """,Long.class,targetFamily,targetFamily));
            assertEquals(2L,b.queryForObject("""
                    SELECT count(*) FROM reward_purchases p
                    JOIN reward_definitions d ON d.id=p.reward_definition_id
                    JOIN family_members m ON m.id=p.member_id
                    WHERE d.family_id=? AND m.family_id=?
                    """,Long.class,targetFamily,targetFamily));
            // Padding-family business rows must survive the target restore.
            assertEquals(1L,b.queryForObject(
                    "SELECT count(*) FROM task_definitions WHERE id=? AND family_id=?",
                    Long.class,paddingTask,paddingFamily));
            assertEquals(1L,b.queryForObject(
                    "SELECT count(*) FROM reward_definitions WHERE id=? AND family_id=?",
                    Long.class,paddingReward,paddingFamily));
            assertEquals(1L,b.queryForObject(
                    "SELECT count(*) FROM workspace_restore_receipts WHERE operation_id=?",
                    Long.class,scope.operationId()));
            assertThrows(Exception.class,()->dstTx.execute(tx->
                    nativeEngine.restore(scope,sealed,key,memberMap,currencyMap)));
            assertEquals(4L,b.queryForObject(
                    "SELECT count(*) FROM point_transactions WHERE member_id=?",
                    Long.class,targetMember));
        }
    }
}
