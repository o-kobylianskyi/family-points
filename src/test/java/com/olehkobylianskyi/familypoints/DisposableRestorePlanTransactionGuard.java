package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.security.WorkspaceDestructivePermitContract;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Backend B / B-B-07: TEST ONLY durable plan-consumption rehearsal.
 * Descriptors are caller supplied and NOT verified issuer credentials.
 * This does not consume C's real owner permit or write production restore.
 */
final class DisposableRestorePlanTransactionGuard {
    record Receipt(UUID operationId,String planDigest,long targetEpochBefore,
                   long targetEpochAfter) {}
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final DisposableRestorePlanFingerprint fingerprints=
            new DisposableRestorePlanFingerprint();

    DisposableRestorePlanTransactionGuard(JdbcTemplate db,PlatformTransactionManager manager) {
        this.db=Objects.requireNonNull(db);
        this.tx=new TransactionTemplate(Objects.requireNonNull(manager));
    }

    Receipt consume(DisposableRestorePlanFingerprint.Plan plan,
            WorkspaceDestructivePermitContract.Scope supplied,
            WorkspaceDestructivePermitContract.Scope issued,
            Instant now,boolean injectedFailureAfterMutation) {
        var actual=fingerprints.calculate(plan);
        fingerprints.requireMatchingScope(plan,actual,supplied,issued,now);
        return tx.execute(status->{
            // Deliberately identical B/C lock order: family epoch first,
            // then the operation uniqueness row. Full C permit consumption
            // needs approved shared migration/issuer/owner authentication.
            var epochs=db.queryForList(
                    "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                    Long.class,plan.targetWorkspace());
            if(epochs.size()!=1
                    || !Objects.equals(plan.targetExpectedEpoch(),epochs.get(0)))
                throw new SecurityException("RESTORE_PLAN_TARGET_EPOCH_STALE");
            int inserted=db.update("""
                    INSERT INTO qa_restore_plan_operations(
                        operation_id,target_family,plan_digest,before_epoch,after_epoch)
                    VALUES (?,?,?,?,?)
                    ON CONFLICT (operation_id) DO NOTHING
                    """,supplied.operationId(),plan.targetWorkspace(),actual.planDigest(),
                    plan.targetExpectedEpoch(),plan.targetExpectedEpoch()+1);
            if(inserted!=1)
                throw new SecurityException("RESTORE_PLAN_OPERATION_ALREADY_USED");
            db.update("""
                    INSERT INTO qa_restore_plan_events(operation_id,target_family,category)
                    VALUES (?,?,?)
                    """,supplied.operationId(),plan.targetWorkspace(),"SYNTHETIC_RESTORE");
            if(injectedFailureAfterMutation)
                throw new IllegalStateException("QA_FAIL_AFTER_SYNTHETIC_RESTORE_WRITE");
            int advanced=db.update("UPDATE families SET data_epoch=data_epoch+1 WHERE id=? AND data_epoch=?",
                    plan.targetWorkspace(),plan.targetExpectedEpoch());
            if(advanced!=1)
                throw new SecurityException("RESTORE_PLAN_EPOCH_ADVANCE_CONFLICT");
            return new Receipt(supplied.operationId(),actual.planDigest(),
                    plan.targetExpectedEpoch(),plan.targetExpectedEpoch()+1);
        });
    }
}
