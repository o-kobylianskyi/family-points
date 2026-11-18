package com.olehkobylianskyi.familypoints.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Durable attempt budget in an independent transaction. It commits before a
 * password-denied parent transaction rolls back. No RESET budget is reused.
 */
@Service
public class WorkspaceRestorePasswordLimiter {
    private final JdbcTemplate db;
    public WorkspaceRestorePasswordLimiter(JdbcTemplate db) { this.db=db; }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void requireAvailable(long workspace,long account) {
        if(workspace<=0||account<=0) throw new SecurityException("RESTORE_REAUTH_SCOPE_INVALID");
        db.update("""
            INSERT INTO workspace_restore_reauth_limits
                (workspace_id,account_id,window_started_at,failed_attempts)
            VALUES (?,?,clock_timestamp(),0)
            ON CONFLICT (workspace_id,account_id) DO NOTHING
            """,workspace,account);
        Integer failures=db.queryForObject("""
            SELECT CASE WHEN window_started_at>clock_timestamp()-interval '15 minutes'
                        THEN failed_attempts ELSE 0 END
            FROM workspace_restore_reauth_limits
            WHERE workspace_id=? AND account_id=? FOR UPDATE
            """,Integer.class,workspace,account);
        if(failures==null||failures>=5)
            throw new SecurityException("RESTORE_REAUTH_RATE_LIMIT");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void recordFailure(long workspace,long account) {
        if(workspace<=0||account<=0) throw new SecurityException("RESTORE_REAUTH_SCOPE_INVALID");
        db.update("""
            INSERT INTO workspace_restore_reauth_limits
                (workspace_id,account_id,window_started_at,failed_attempts)
            VALUES (?,?,clock_timestamp(),1)
            ON CONFLICT (workspace_id,account_id) DO UPDATE
            SET failed_attempts = CASE
                  WHEN workspace_restore_reauth_limits.window_started_at <
                       clock_timestamp()-interval '15 minutes' THEN 1
                  ELSE LEAST(10,workspace_restore_reauth_limits.failed_attempts+1) END,
                window_started_at = CASE
                  WHEN workspace_restore_reauth_limits.window_started_at <
                       clock_timestamp()-interval '15 minutes' THEN clock_timestamp()
                  ELSE workspace_restore_reauth_limits.window_started_at END
            """,workspace,account);
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void clearSuccess(long workspace,long account) {
        db.update("""
            UPDATE workspace_restore_reauth_limits
            SET failed_attempts=0,window_started_at=clock_timestamp()
            WHERE workspace_id=? AND account_id=?
            """,workspace,account);
    }
}
