package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TemplateActivationReceiptServiceTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final WorkspaceEpochTransactionGuard epoch=mock(WorkspaceEpochTransactionGuard.class);
    private final WorkspaceTemplateController templates=mock(WorkspaceTemplateController.class);
    private final TemplateActivationReceiptService service=
            new TemplateActivationReceiptService(db,epoch,templates);

    @Test void rejectsMissingKeyAndRequestBeforeAnyMutation() {
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->service.activateTask(1,3,null,new TaskDefinitionCreateRequest())).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ResponseStatusException.class,
                ()->service.activateTask(1,3,UUID.randomUUID(),null)).getStatusCode());
        verifyNoInteractions(db,epoch,templates);
    }

    @Test void missingReceiptReturns404AndNeverActivates() {
        when(db.query(contains("FROM template_activation_receipts"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(1L),any(UUID.class)))
                .thenReturn(List.of());
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
                ()->service.receipt(1L,UUID.randomUUID())).getStatusCode());
        verifyNoInteractions(epoch,templates);
        verify(db,never()).update(anyString(),any(Object[].class));
    }
}
