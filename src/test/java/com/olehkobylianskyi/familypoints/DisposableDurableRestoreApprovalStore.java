package com.olehkobylianskyi.familypoints;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

/**
 * TEST ONLY. SQL table is explicitly created in a disposable PG18 test DB,
 * not in Flyway or production. Demonstrates transaction-bound epoch check and
 * durable unique operation consumption across helper objects/process-like
 * restarts. The asserted actor/key are synthetic, NOT owner reauthentication.
 */
final class DisposableDurableRestoreApprovalStore {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;

    DisposableDurableRestoreApprovalStore(JdbcTemplate db,PlatformTransactionManager manager) {
        this.db=Objects.requireNonNull(db);
        this.tx=new TransactionTemplate(Objects.requireNonNull(manager));
    }

    /**
     * A disposable financial write and authorization reservation in ONE
     * PostgreSQL transaction. The SQL table exists only in the PG18 fixture.
     * No production point ledger is written, and no real restore is exposed.
     */
    /**
     * Test-only multi-category financial rehearsal. One locked family epoch,
     * one operation reservation and two independent category rows share the
     * SAME PostgreSQL transaction. This never writes production ledgers.
     */
    void consumeWithTwoSyntheticCategories(DisposableRestoreApprovalGate.Permit permit,
            long sourceFamily,long targetFamily,UUID snapshot,UUID operation,
            long claimedActor,Instant now,Map<Long,Long> explicitMappings,
            SecretKey reviewKey,long firstOriginalMember,long secondOriginalMember,
            int pointAmount,int reputationAmount,int failAfterStep) {
        tx.executeWithoutResult(status->{
            Long epoch=db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class,targetFamily);
            if(epoch==null)
                throw new SecurityException("RESTORE_APPROVAL_UNKNOWN_TARGET");
            new DisposableRestoreApprovalGate().consume(permit,sourceFamily,
                    targetFamily,snapshot,operation,claimedActor,epoch,now,
                    explicitMappings,reviewKey);
            Long pointMember=explicitMappings.get(firstOriginalMember);
            Long reputationMember=explicitMappings.get(secondOriginalMember);
            if(pointMember==null || reputationMember==null
                    || pointAmount<=0 || reputationAmount<=0)
                throw new SecurityException("RESTORE_FINANCIAL_MAPPING_INVALID");
            int inserted=db.update("""
                    INSERT INTO qa_restore_approval_consumption
                        (operation_id,source_family,target_family,snapshot_id,actor_id,target_epoch)
                    VALUES (?,?,?,?,?,?)
                    ON CONFLICT (operation_id) DO NOTHING
                    """,operation,sourceFamily,targetFamily,snapshot,claimedActor,epoch);
            if(inserted!=1)
                throw new SecurityException("RESTORE_APPROVAL_ALREADY_CONSUMED");
            if(failAfterStep==1)
                throw new IllegalStateException("QA_FAIL_AFTER_APPROVAL");
            db.update("""
                    INSERT INTO qa_restore_financial_rehearsal
                        (operation_id,target_family,target_member,amount)
                    VALUES (?,?,?,?)
                    """,operation,targetFamily,pointMember,pointAmount);
            if(failAfterStep==2)
                throw new IllegalStateException("QA_FAIL_AFTER_POINTS");
            db.update("""
                    INSERT INTO qa_restore_reputation_rehearsal
                        (operation_id,target_family,target_member,amount)
                    VALUES (?,?,?,?)
                    """,operation,targetFamily,reputationMember,reputationAmount);
            if(failAfterStep==3)
                throw new IllegalStateException("QA_FAIL_AFTER_REPUTATION");
        });
    }

    void consumeWithSyntheticFinancialWrite(DisposableRestoreApprovalGate.Permit permit,
            long sourceFamily,long targetFamily,UUID snapshot,UUID operation,
            long claimedActor,Instant now,Map<Long,Long> explicitMappings,
            SecretKey reviewKey,long originalMember,int amount,
            boolean injectFailureAfterFinancialWrite) {
        tx.executeWithoutResult(status->{
            Long lockedEpoch=db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class,targetFamily);
            if(lockedEpoch==null)
                throw new SecurityException("RESTORE_APPROVAL_UNKNOWN_TARGET");
            new DisposableRestoreApprovalGate().consume(permit,sourceFamily,
                    targetFamily,snapshot,operation,claimedActor,lockedEpoch,
                    now,explicitMappings,reviewKey);
            if(!explicitMappings.containsKey(originalMember) || amount<=0)
                throw new SecurityException("RESTORE_FINANCIAL_MAPPING_INVALID");
            int inserted=db.update("""
                    INSERT INTO qa_restore_approval_consumption
                        (operation_id,source_family,target_family,snapshot_id,actor_id,target_epoch)
                    VALUES (?,?,?,?,?,?)
                    ON CONFLICT (operation_id) DO NOTHING
                    """,operation,sourceFamily,targetFamily,snapshot,claimedActor,lockedEpoch);
            if(inserted!=1)
                throw new SecurityException("RESTORE_APPROVAL_ALREADY_CONSUMED");
            db.update("""
                    INSERT INTO qa_restore_financial_rehearsal
                        (operation_id,target_family,target_member,amount)
                    VALUES (?,?,?,?)
                    """,operation,targetFamily,explicitMappings.get(originalMember),amount);
            if(injectFailureAfterFinancialWrite)
                throw new IllegalStateException("QA_INJECTED_FINANCIAL_ROLLBACK");
        });
    }

    void consume(DisposableRestoreApprovalGate.Permit permit,long sourceFamily,
            long targetFamily,UUID snapshot,UUID operation,long claimedActor,
            Instant now,Map<Long,Long> explicitMappings,SecretKey reviewKey,
            boolean injectFailureAfterReservation) {
        tx.executeWithoutResult(status->{
            // Serialize competing operations against the same target family,
            // and compare the epoch INSIDE the same transaction as the INSERT.
            Long lockedEpoch=db.queryForObject(
                    "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class,targetFamily);
            if(lockedEpoch==null)
                throw new SecurityException("RESTORE_APPROVAL_UNKNOWN_TARGET");
            // Fresh instance on every call proves the SQL operation constraint,
            // not a previous Java instance's in-memory consumed set.
            new DisposableRestoreApprovalGate().consume(permit,sourceFamily,
                    targetFamily,snapshot,operation,claimedActor,
                    lockedEpoch,now,explicitMappings,reviewKey);
            int inserted=db.update("""
                    INSERT INTO qa_restore_approval_consumption
                        (operation_id,source_family,target_family,snapshot_id,actor_id,target_epoch)
                    VALUES (?,?,?,?,?,?)
                    ON CONFLICT (operation_id) DO NOTHING
                    """,operation,sourceFamily,targetFamily,snapshot,claimedActor,lockedEpoch);
            if(inserted!=1)
                throw new SecurityException("RESTORE_APPROVAL_ALREADY_CONSUMED");
            if(injectFailureAfterReservation)
                throw new IllegalStateException("QA_INJECTED_ROLLBACK");
        });
    }
}
