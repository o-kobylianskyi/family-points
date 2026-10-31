package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceSetupServiceTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final WorkspaceEpochTransactionGuard guard=mock(WorkspaceEpochTransactionGuard.class);

    @Test void disabledNeverReadsOrWrites() {
        var service=new WorkspaceSetupService(db,guard,false);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class,()->service.current(1L)).getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class,()->service.setStep(1L,"MEMBERS",
                        new WorkspaceSetupService.StepRequest("COMPLETE",0L))).getStatusCode());
        verifyNoInteractions(db,guard);
    }

    @Test void rejectsInvalidStepBeforeLockOrWrite() {
        var service=new WorkspaceSetupService(db,guard,true);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class,()->service.setStep(1L,"NOT_A_STEP",
                        new WorkspaceSetupService.StepRequest("COMPLETE",0L))).getStatusCode());
        verifyNoInteractions(db,guard);
    }

    @Test void rejectsUnrecognizedStatusBeforeLockOrWrite() {
        var service=new WorkspaceSetupService(db,guard,true);
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class,()->service.setStep(1L,"MEMBERS",
                        new WorkspaceSetupService.StepRequest("PENDING",0L))).getStatusCode());
        verifyNoInteractions(db,guard);
    }
}
