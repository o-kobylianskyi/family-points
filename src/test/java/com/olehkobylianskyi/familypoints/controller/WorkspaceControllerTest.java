package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceControllerTest {
    @Test
    void listOnlyReturnsTheSignedInFamily() {
        WorkspaceService service = mock(WorkspaceService.class);
        CurrentUserService current = mock(CurrentUserService.class);
        WorkspaceController controller = new WorkspaceController(service, current);
        when(current.getCurrentWorkspaceId()).thenReturn(17L);
        when(service.getWorkspaceById(17L)).thenReturn(Optional.of(new Workspace("Family A")));

        var results = controller.getAllWorkspaces();

        assertEquals(1, results.size());
        assertEquals("Family A", results.getFirst().getName());
        verify(service, never()).getAllWorkspaces();
    }

    @Test
    void genericCreateIsDisabledUntilSecureFamilyRegistration() {
        WorkspaceController controller = new WorkspaceController(
                mock(WorkspaceService.class), mock(CurrentUserService.class));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                controller::createWorkspace);
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void genericDeleteIsDisabled() {
        WorkspaceController controller = new WorkspaceController(
                mock(WorkspaceService.class), mock(CurrentUserService.class));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.deleteWorkspace(50L));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }
}
