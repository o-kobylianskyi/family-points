package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.TemplateCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceTemplateControllerTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final WorkspaceTemplateController controller =
            new WorkspaceTemplateController(db, new TemplateCatalogService());

    @Test void rejectsMalformedDefinitionWithoutWriting() {
        var invalid = new WorkspaceTemplateController.TemplateRequest("Test", "{bad", "TASK");
        assertThrows(ResponseStatusException.class, () -> controller.create(1L, invalid));
        verifyNoInteractions(db);
    }

    @Test void rejectsUnsupportedKindsWithoutWriting() {
        var invalid = new WorkspaceTemplateController.TemplateRequest("Test", "{}", "ACCOUNT");
        assertThrows(ResponseStatusException.class, () -> controller.create(1L, invalid));
        verifyNoInteractions(db);
    }

    @Test void emptyImportDoesNotWrite() {
        var result = controller.importSelected(1L, new WorkspaceTemplateController.ImportRequest(List.of()));
        assertTrue(result.selectedCodes().isEmpty());
        assertTrue(result.toImport().isEmpty());
        verifyNoInteractions(db);
    }

    @Test void unknownCatalogCodeDoesNotWrite() {
        assertThrows(ResponseStatusException.class,
                () -> controller.importSelected(1L,
                        new WorkspaceTemplateController.ImportRequest(List.of("unknown.template"))));
        verifyNoInteractions(db);
    }
}
