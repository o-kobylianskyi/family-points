package com.olehkobylianskyi.familypoints.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Independent commit: invalid credentials must not roll back their attempt count. */
@Service
public class ResetChallengeAttemptLimiter {
    private final JdbcTemplate db;
    public ResetChallengeAttemptLimiter(JdbcTemplate db) { this.db=db; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndRecord(long workspaceId,long accountId) {
        Integer attempts=db.queryForObject("""
                INSERT INTO workspace_reset_reauth_limits
                  (workspace_id,account_id,window_started_at,failed_attempts)
                VALUES (?,?,now(),1)
                ON CONFLICT (workspace_id,account_id) DO UPDATE SET
                  failed_attempts=CASE
                    WHEN workspace_reset_reauth_limits.window_started_at <= now() - interval '15 minutes'
                      THEN 1
                    ELSE LEAST(workspace_reset_reauth_limits.failed_attempts + 1, 6)
                  END,
                  window_started_at=CASE
                    WHEN workspace_reset_reauth_limits.window_started_at <= now() - interval '15 minutes'
                      THEN now()
                    ELSE workspace_reset_reauth_limits.window_started_at
                  END
                RETURNING failed_attempts
                """,Integer.class,workspaceId,accountId);
        if(attempts==null || attempts>5)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"RESET_REAUTH_RATE_LIMIT");
    }
}
