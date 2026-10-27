package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceDataResetPrepareControllerTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final WorkspaceDataResetPreviewController inventory=mock(WorkspaceDataResetPreviewController.class);
    private final WorkspaceDataResetPrepareController controller=
            new WorkspaceDataResetPrepareController(db,current,inventory);

    @Test void wrongWorkspaceRejectedBeforeDatabaseOrInventory() {
        when(current.belongsToWorkspace(7L)).thenReturn(false);
        var request=new WorkspaceDataResetPrepareController.PrepareRequest(
                UUID.randomUUID(),Set.of("PRIVATE_TEMPLATES"));
        assertThrows(ResponseStatusException.class,()->controller.prepare(7L,request));
        verifyNoInteractions(db,inventory);
    }

    @Test void replayConflictPreventsMutationOrExecution() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        when(current.getCurrentMemberId()).thenReturn(17L);
        when(inventory.plan(eq(7L),any())).thenReturn(
                new WorkspaceDataResetPreviewController.ResetPlan(
                        7L,Set.of("PRIVATE_TEMPLATES"),false,java.util.List.of("execution disabled"),
                        java.util.List.of("Account authentication")));
        when(inventory.preview(7L)).thenReturn(
                new WorkspaceDataResetPreviewController.ResetPreview(
                        7L,false,false,java.util.List.of(),Map.of("PRIVATE_TEMPLATES",
                            new WorkspaceDataResetPreviewController.ResetCategory(
                                    "PRIVATE_TEMPLATES",2L,false,"not executable"))));
        when(db.queryForMap(contains("workspace_reset_operations"),eq(7L),anyString())).thenReturn(
                Map.of("requested_by_member_id",17L,"selected_categories_json","[]",
                        "snapshot_digest","a".repeat(64),"status","PREPARED"));
        var request=new WorkspaceDataResetPrepareController.PrepareRequest(
                UUID.randomUUID(),Set.of("PRIVATE_TEMPLATES"));
        assertEquals(org.springframework.http.HttpStatus.CONFLICT,
                assertThrows(ResponseStatusException.class,
                        ()->controller.prepare(7L,request)).getStatusCode());
        verify(db).update(contains("INSERT INTO workspace_reset_operations"),any(Object[].class));
        verify(db,never()).update(contains("DELETE"),any(Object[].class));
    }
}
