package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.*;
import com.olehkobylianskyi.familypoints.service.PointMutationService;
import com.olehkobylianskyi.familypoints.service.PointService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PointMutationEpochControllerTest {
    @Test void everyMutationRouteForwardsSubmittedEpochToTransactionBoundary() throws Exception {
        var points = mock(PointService.class);
        var mutations = mock(PointMutationService.class);
        var stale = new ResponseStatusException(HttpStatus.CONFLICT);
        when(mutations.applyManualOperation(eq(7L), eq(9L), anyString(), any(), eq(3L))).thenThrow(stale);
        when(mutations.applyTypedManualOperation(eq(7L), eq(9L), anyString(), any(), eq(3L))).thenThrow(stale);
        when(mutations.applyOfflineOperation(eq(7L), eq(9L), anyString(), any(), eq(3L))).thenThrow(stale);
        var mvc = MockMvcBuilders.standaloneSetup(new PointController(points, mutations)).build();
        String body = """
                {"amount":5,"pointTypeId":11,"operationId":"af2d775b-9f26-45eb-8812-46f165e722d1"}
                """;
        for (String route : new String[]{"earn", "spend", "penalty", "manual/earn", "offline/earn"}) {
            mvc.perform(post("/workspaces/7/members/9/points/" + route)
                    .header("X-Workspace-Data-Epoch", "3")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
        }
        for (String operation : new String[]{"earn", "spend", "penalty"}) {
            verify(mutations).applyManualOperation(eq(7L), eq(9L), eq(operation),
                    any(PointOperationRequest.class), eq(3L));
        }
        verify(mutations).applyTypedManualOperation(eq(7L), eq(9L), eq("earn"),
                any(PointOperationTypedRequest.class), eq(3L));
        verify(mutations).applyOfflineOperation(eq(7L), eq(9L), eq("earn"),
                any(OfflinePointOperationRequest.class), eq(3L));
        verifyNoInteractions(points);
    }
}
