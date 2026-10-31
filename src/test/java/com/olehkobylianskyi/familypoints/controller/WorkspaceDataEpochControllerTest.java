package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceDataEpochControllerTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final CurrentUserService current=mock(CurrentUserService.class);
    private final WorkspaceDataEpochController controller=new WorkspaceDataEpochController(db,current);

    @Test void deniesOtherWorkspaceWithoutDatabaseRead() {
        when(current.belongsToWorkspace(7L)).thenReturn(false);
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,()->controller.current(7L)).getStatusCode());
        verifyNoInteractions(db);
    }

    @Test void returnsCurrentEpochWithoutMutation() {
        when(current.belongsToWorkspace(7L)).thenReturn(true);
        when(db.queryForObject("SELECT data_epoch FROM families WHERE id=?",Long.class,7L))
                .thenReturn(0L,2L);
        assertEquals(0L,controller.current(7L).dataEpoch());
        assertEquals(2L,controller.current(7L).dataEpoch());
        verify(db,never()).update(anyString(),any(Object[].class));
    }
}
