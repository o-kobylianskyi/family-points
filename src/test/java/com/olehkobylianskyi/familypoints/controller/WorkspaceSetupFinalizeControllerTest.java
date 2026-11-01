package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.WorkspaceSetupFinalizeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Controller contract, no database writes or mocked authorization bypass claims. */
class WorkspaceSetupFinalizeControllerTest {
    private final WorkspaceSetupFinalizeService service=mock(WorkspaceSetupFinalizeService.class);
    private final WorkspaceSetupFinalizeController controller=new WorkspaceSetupFinalizeController(service);

    @Test void returnsNoStoreAndPreservesExactIdempotencyIdentity() {
        UUID id=UUID.randomUUID();
        var request=new WorkspaceSetupFinalizeService.Request(7L,List.of("task.a"));
        var preview=new WorkspaceTemplateController.ImportPreview(List.of("task.a"),List.of(),List.of("task.a"));
        var result=new WorkspaceSetupFinalizeService.Result(id,false,42L,0L,8L,"COMPLETED",preview);
        when(service.finalizeWizard(42L,id,request)).thenReturn(result);
        var response=controller.finalizeImport(42L,id,request);
        assertEquals(200,response.getStatusCode().value());
        assertEquals(result,response.getBody());
        assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
        verify(service).finalizeWizard(42L,id,request);
        verifyNoMoreInteractions(service);
    }

    @Test void receiptIsScopedToPathWorkspaceAndNeverQueriesOtherFamily() {
        UUID id=UUID.randomUUID();
        var result=new WorkspaceSetupFinalizeService.Result(id,true,42L,0L,8L,
                "COMPLETED",new WorkspaceTemplateController.ImportPreview(List.of(),List.of(),List.of()));
        when(service.receipt(42L,id)).thenReturn(result);
        var response=controller.receipt(42L,id);
        assertEquals(result,response.getBody());
        assertTrue(response.getHeaders().getCacheControl().contains("no-store"));
        verify(service).receipt(42L,id);
        verifyNoMoreInteractions(service);
    }

    @Test void propagatesRefusalWithoutFabricatingSuccessfulReceipt() {
        UUID id=UUID.randomUUID();
        var request=new WorkspaceSetupFinalizeService.Request(0L,List.of());
        when(service.finalizeWizard(42L,id,request))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_STEPS_INCOMPLETE"));
        var failure=assertThrows(ResponseStatusException.class,
                ()->controller.finalizeImport(42L,id,request));
        assertEquals(HttpStatus.CONFLICT,failure.getStatusCode());
        verify(service).finalizeWizard(42L,id,request);
    }
}
