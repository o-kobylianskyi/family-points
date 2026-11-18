package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResetChallengeAttemptLimiterTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final ResetChallengeAttemptLimiter limiter=new ResetChallengeAttemptLimiter(db);

    @Test void permitsFirstFiveAttemptsAndRejectsSixth() {
        when(db.queryForObject(contains("workspace_reset_reauth_limits"),
                eq(Integer.class),eq(7L),eq(23L))).thenReturn(1,2,3,4,5,6);
        for(int i=1;i<=5;i++) assertTrue(limiter.checkAndRecord(7L,23L)<=5);
        assertEquals(6,limiter.checkAndRecord(7L,23L));
        verify(db,times(6)).queryForObject(contains("workspace_reset_reauth_limits"),
                eq(Integer.class),eq(7L),eq(23L));
    }
    @Test void databaseFailureDoesNotPermitChallengeAttempt() {
        when(db.queryForObject(contains("workspace_reset_reauth_limits"),
                eq(Integer.class),eq(7L),eq(23L)))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("DB unavailable"));
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,
                () -> limiter.checkAndRecord(7L,23L));
    }

    @Test void missingCounterResultFailsClosed() {
        when(db.queryForObject(contains("workspace_reset_reauth_limits"),
                eq(Integer.class),eq(7L),eq(23L)))
                .thenReturn((Integer) null);
        assertEquals(6, limiter.checkAndRecord(7L,23L));
    }

    @Test void limiterSeparatesAccountsAndWorkspaces() {
        when(db.queryForObject(contains("workspace_reset_reauth_limits"),
                eq(Integer.class),eq(7L),eq(23L))).thenReturn(6);
        when(db.queryForObject(contains("workspace_reset_reauth_limits"),
                eq(Integer.class),eq(8L),eq(23L))).thenReturn(1);
        when(db.queryForObject(contains("workspace_reset_reauth_limits"),
                eq(Integer.class),eq(7L),eq(24L))).thenReturn(1);
        assertEquals(6, limiter.checkAndRecord(7L,23L));
        assertEquals(1, limiter.checkAndRecord(8L,23L));
        assertEquals(1, limiter.checkAndRecord(7L,24L));
    }

}
