package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Row-level epoch barrier for the shared transaction that performs a workspace write.
 * Call before any business mutation, inside that same transaction. An HTTP-only
 * epoch check is insufficient: reset and writes must serialize on families.id.
 */
@Service
public class WorkspaceEpochTransactionGuard {
    private final JdbcTemplate db;

    public WorkspaceEpochTransactionGuard(JdbcTemplate db) { this.db=db; }

    /**
     * Client-originated service write: preserve the submitted epoch all the way
     * into this transaction. Absence of an HTTP request is NOT a current-epoch bypass.
     */
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
     * Only server-derived work may use this entry point. Such work must read
     * its entities AFTER acquiring the family lock. HTTP callers still need
     * their submitted epoch, including administrator-triggered deadline processing.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long requireCurrentRequestOrSystemEpoch(long workspaceId) {
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() != null)
            return requireCurrentRequestEpoch(workspaceId);
        return lockCurrentEpoch(workspaceId);
    }

    /** Current-state refresh, not permission to replay a client mutation. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long lockCurrentEpoch(long workspaceId) {
        return lockedEpoch(workspaceId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long requireCurrentEpoch(long workspaceId, Long expectedEpoch) {
        Long actual=lockedEpoch(workspaceId);
        if(expectedEpoch==null && actual==0L) return actual;
        if(expectedEpoch==null || expectedEpoch<0 || !expectedEpoch.equals(actual))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"WORKSPACE_DATA_EPOCH_STALE");
        return actual;
    }

    private long lockedEpoch(long workspaceId) {
        Long actual=db.query(
                "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                rs -> rs.next()?rs.getLong(1):null, workspaceId);
        if(actual==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workspace not found");
        return actual;
    }
}
