package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceResetChallengeControllerTest {
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final WorkspaceResetChallengeController controller=new WorkspaceResetChallengeController(current);

    @Test void challengeNeverIssuesProofOrExecutes() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var request=new WorkspaceResetChallengeController.ChallengeRequest(UUID.randomUUID(),"a".repeat(64));
        assertEquals(HttpStatus.LOCKED,
                assertThrows(ResponseStatusException.class,()->controller.challenge(7L,request)).getStatusCode());
        assertEquals(HttpStatus.LOCKED,
                assertThrows(ResponseStatusException.class,()->controller.execute(7L)).getStatusCode());
    }

    @Test void wrongTenantIsForbiddenAndInvalidDigestRejected() {
        when(current.belongsToWorkspace(7L)).thenReturn(false);
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,()->controller.execute(7L)).getStatusCode());
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        var bad=new WorkspaceResetChallengeController.ChallengeRequest(UUID.randomUUID(),"invalid");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class,()->controller.challenge(7L,bad)).getStatusCode());
    }
}
