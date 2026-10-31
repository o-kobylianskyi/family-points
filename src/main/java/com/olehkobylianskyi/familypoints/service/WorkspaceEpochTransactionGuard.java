package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;

/** Family lock and original-epoch validation in the transaction that performs the write. */
@Service
public class WorkspaceEpochTransactionGuard {
    private static final Object VALIDATED_EPOCHS = new Object();
    private final JdbcTemplate db;

    public WorkspaceEpochTransactionGuard(JdbcTemplate db) { this.db = db; }

    /** Persistent REQUIRES_NEW reauth throttling must run before this family's lock. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireUnfencedReauthTransaction(long workspaceId) {
        var epochs = validatedEpochs();
        if (epochs != null && epochs.containsKey(workspaceId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RESET_REAUTH_NEW_TRANSACTION_REQUIRED");
    }

    /** Missing HTTP context never invents a current client epoch. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long requireCurrentRequestEpoch(long workspaceId) {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String header = attributes instanceof org.springframework.web.context.request.ServletRequestAttributes request
                ? request.getRequest().getHeader("X-Workspace-Data-Epoch") : null;
        Long expected = null;
        if (header != null) {
            try {
                expected = Long.valueOf(header);
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "WORKSPACE_DATA_EPOCH_STALE");
            }
        }
        return requireCurrentEpoch(workspaceId, expected);
    }

    /**
     * Internal ledger writes may join an already validated transaction for the
     * SAME family. HTTP calls always recheck their original header. Without a
     * validated parent, absence of context is accepted only at legacy epoch zero.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long requireCurrentRequestOrLockedEpoch(long workspaceId) {
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null)
            return requireCurrentRequestEpoch(workspaceId);
        var epochs = validatedEpochs();
        return requireCurrentEpoch(workspaceId, epochs == null ? null : epochs.get(workspaceId));
    }

    /**
     * Only server-derived work may begin with current state. Reload domain
     * entities AFTER this lock. HTTP-triggered system work still needs its header.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long requireCurrentRequestOrSystemEpoch(long workspaceId) {
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null)
            return requireCurrentRequestEpoch(workspaceId);
        return lockCurrentEpoch(workspaceId);
    }

    /** Current-state refresh, never proof for replaying an old client mutation. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long lockCurrentEpoch(long workspaceId) {
        long actual = lockedEpoch(workspaceId);
        rememberValidated(workspaceId, actual);
        return actual;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long requireCurrentEpoch(long workspaceId, Long expectedEpoch) {
        long actual = lockedEpoch(workspaceId);
        if (!(expectedEpoch == null && actual == 0L)
                && (expectedEpoch == null || expectedEpoch < 0 || expectedEpoch.longValue() != actual))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "WORKSPACE_DATA_EPOCH_STALE");
        rememberValidated(workspaceId, actual);
        return actual;
    }

    private long lockedEpoch(long workspaceId) {
        Long actual = db.query(
                "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                rs -> rs.next() ? rs.getLong(1) : null, workspaceId);
        if (actual == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        return actual;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> validatedEpochs() {
        return (Map<Long, Long>) TransactionSynchronizationManager.getResource(VALIDATED_EPOCHS);
    }

    private void rememberValidated(long workspaceId, long epoch) {
        // Production entry points are MANDATORY proxies; bare unit fixtures need no resource state.
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) return;
        var existing = validatedEpochs();
        if (existing != null) {
            existing.put(workspaceId, epoch);
            return;
        }
        var created = new HashMap<Long, Long>();
        created.put(workspaceId, epoch);
        TransactionSynchronizationManager.bindResource(VALIDATED_EPOCHS, created);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void suspend() {
                if (TransactionSynchronizationManager.getResource(VALIDATED_EPOCHS) == created)
                    TransactionSynchronizationManager.unbindResource(VALIDATED_EPOCHS);
            }
            @Override public void resume() {
                TransactionSynchronizationManager.bindResource(VALIDATED_EPOCHS, created);
            }
            @Override public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.getResource(VALIDATED_EPOCHS) == created)
                    TransactionSynchronizationManager.unbindResource(VALIDATED_EPOCHS);
            }
        });
    }
}
