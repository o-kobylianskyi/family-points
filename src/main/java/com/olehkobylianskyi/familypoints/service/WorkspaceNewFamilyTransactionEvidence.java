package com.olehkobylianskyi.familypoints.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Ephemeral internal evidence that a family and its initial member were
 * provisioned by the current service in the CURRENT physical transaction.
 * No JWT/role/clock-based legacy backfill is accepted.
 */
final class WorkspaceNewFamilyTransactionEvidence {
    private static final Object KEY=new Object();
    private WorkspaceNewFamilyTransactionEvidence() {}

    static void record(long workspace,long member) {
        if(workspace<=0||member<=0
                ||!TransactionSynchronizationManager.isActualTransactionActive()
                ||!TransactionSynchronizationManager.isSynchronizationActive())
            throw new SecurityException("RESTORE_CREATOR_CREATION_TRANSACTION_REQUIRED");
        @SuppressWarnings("unchecked")
        Map<Long,Long> marked=(Map<Long,Long>)TransactionSynchronizationManager.getResource(KEY);
        if(marked==null) {
            marked=new HashMap<>();
            final Map<Long,Long> state=marked;
            TransactionSynchronizationManager.bindResource(KEY,state);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void suspend() {
                    if(TransactionSynchronizationManager.getResource(KEY)==state)
                        TransactionSynchronizationManager.unbindResource(KEY);
                }
                @Override public void resume() {
                    TransactionSynchronizationManager.bindResource(KEY,state);
                }
                @Override public void afterCompletion(int status) {
                    if(TransactionSynchronizationManager.getResource(KEY)==state)
                        TransactionSynchronizationManager.unbindResource(KEY);
                }
            });
        }
        if(marked.putIfAbsent(workspace,member)!=null)
            throw new SecurityException("RESTORE_CREATOR_CREATION_DUPLICATE");
    }

    static void consume(long workspace,long member) {
        @SuppressWarnings("unchecked")
        Map<Long,Long> marked=(Map<Long,Long>)TransactionSynchronizationManager.getResource(KEY);
        if(marked==null||!Long.valueOf(member).equals(marked.remove(workspace)))
            throw new SecurityException("RESTORE_CREATOR_NOT_NEW_FAMILY_TRANSACTION");
    }
}
