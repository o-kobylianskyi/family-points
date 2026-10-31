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
}
