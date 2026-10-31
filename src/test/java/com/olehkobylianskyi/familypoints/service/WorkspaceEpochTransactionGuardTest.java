package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceEpochTransactionGuardTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final WorkspaceEpochTransactionGuard guard=new WorkspaceEpochTransactionGuard(db);

    private void epoch(Long value) {
        when(db.query(eq("SELECT data_epoch FROM families WHERE id=? FOR UPDATE"),
                any(ResultSetExtractor.class),eq(7L))).thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    ResultSetExtractor<Long> extractor=invocation.getArgument(1);
                    ResultSet rs=mock(ResultSet.class);
                    when(rs.next()).thenReturn(value!=null);
                    if(value!=null) when(rs.getLong(1)).thenReturn(value);
                    return extractor.extractData(rs);
                });
    }

    @Test void legacyWithoutHeaderIsAllowedOnlyAtZero() {
        epoch(0L);
        assertEquals(0,guard.requireCurrentEpoch(7L,null));
        epoch(2L);
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->guard.requireCurrentEpoch(7L,null)).getStatusCode());
    }

    @Test void rejectsStaleAndAllowsCurrentEpochWithRowLock() {
        epoch(3L);
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->guard.requireCurrentEpoch(7L,2L)).getStatusCode());
        assertEquals(3L,guard.requireCurrentEpoch(7L,3L));
        verify(db,times(2)).query(eq("SELECT data_epoch FROM families WHERE id=? FOR UPDATE"),
                any(ResultSetExtractor.class),eq(7L));
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void missingWorkspaceDoesNotPermitWrite() {
        epoch(null);
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->guard.requireCurrentEpoch(7L,0L)).getStatusCode());
    }
}
