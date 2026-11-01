package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceSetupService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceSetupReviewControllerTest {
    private final WorkspaceSetupService setup=mock(WorkspaceSetupService.class);
    private final WorkspaceTemplateController templates=mock(WorkspaceTemplateController.class);
    private final WorkspaceSetupReviewController controller=new WorkspaceSetupReviewController(setup,templates);

    @Test void invalidBodyCannotReadOrWrite() {
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->controller.review(7L,null)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->controller.review(7L,new WorkspaceSetupReviewController.ReviewRequest(0L,null))).getStatusCode());
        verifyNoInteractions(setup,templates);
    }

    @Test void staleRevisionRejectsBeforeImportPreview() {
        when(setup.current(7L)).thenReturn(new WorkspaceSetupService.SetupState(
                7L,0L,3L,"IN_PROGRESS",Map.of(),null));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->controller.review(7L,new WorkspaceSetupReviewController.ReviewRequest(2L,List.of()))).getStatusCode());
        verifyNoInteractions(templates);
    }

    @Test void completedSetupRejectsBeforePreview() {
        when(setup.current(7L)).thenReturn(new WorkspaceSetupService.SetupState(
                7L,0L,3L,"COMPLETED",Map.of(),null));
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,
                ()->controller.review(7L,new WorkspaceSetupReviewController.ReviewRequest(3L,List.of()))).getStatusCode());
        verifyNoInteractions(templates);
    }

    @Test void previewReportsExactCountsAndNeverImports() {
        when(setup.current(7L)).thenReturn(new WorkspaceSetupService.SetupState(
                7L,0L,3L,"IN_PROGRESS",Map.of("MEMBERS","COMPLETE"),null));
        when(templates.preview(eq(7L),any())).thenReturn(new WorkspaceTemplateController.ImportPreview(
                List.of("task.a","reward.b"),List.of("task.a"),List.of("reward.b")));
        var result=controller.review(7L,new WorkspaceSetupReviewController.ReviewRequest(
                3L,List.of("task.a","reward.b"))).getBody();
        assertNotNull(result);
        assertEquals(2,result.selectedCount());
        assertEquals(1,result.alreadyImportedCount());
        assertEquals(1,result.toImportCount());
        assertTrue(result.requiresExplicitConfirmation());
        assertFalse(result.createsActiveDefinitions());
        assertEquals(3L,result.revision());
        verify(templates,never()).importSelected(anyLong(),any());
        verifyNoMoreInteractions(templates);
    }
}
