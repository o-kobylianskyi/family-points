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

    @Transactional(propagation = Propagation.MANDATORY)
    public long requireCurrentEpoch(long workspaceId, Long expectedEpoch) {
        Long actual=db.query(
                "SELECT data_epoch FROM families WHERE id=? FOR UPDATE",
                rs -> rs.next()?rs.getLong(1):null, workspaceId);
        if(actual==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Workspace not found");
        if(expectedEpoch==null && actual==0L) return actual;
        if(expectedEpoch==null || expectedEpoch<0 || !expectedEpoch.equals(actual))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"WORKSPACE_DATA_EPOCH_STALE");
        return actual;
    }
}
