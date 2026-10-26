package com.olehkobylianskyi.familypoints.controller;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceDataResetPreviewControllerTest {
    @Test void previewIsScopedReadOnlyAndNeverOffersDeletion() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        when(db.queryForObject(contains("FROM families"), eq(Integer.class), eq(7L)))
                .thenReturn(1);
        when(db.queryForObject(contains("workspace_private_templates"), eq(Long.class), eq(7L)))
                .thenReturn(2L);
        when(db.queryForObject(contains("task_definitions"), eq(Long.class), eq(7L)))
                .thenReturn(4L);
        when(db.queryForObject(contains("reward_definitions"), eq(Long.class), eq(7L)))
                .thenReturn(3L);

        when(db.queryForObject(contains("FROM task_instances i"),eq(Long.class),eq(7L)))
                .thenReturn(5L);
        when(db.queryForObject(contains("FROM reward_requests"),eq(Long.class),eq(7L)))
                .thenReturn(6L);
        when(db.queryForObject(contains("FROM reward_purchases p"),eq(Long.class),eq(7L)))
                .thenReturn(7L);
        when(db.queryForObject(contains("FROM point_transactions t"),eq(Long.class),eq(7L)))
                .thenReturn(8L);
        when(db.queryForObject(contains("FROM reputation_transactions t"),eq(Long.class),eq(7L)))
                .thenReturn(9L);
        var preview = new WorkspaceDataResetPreviewController(db).preview(7L);
        assertEquals(7L, preview.workspaceId());
        assertFalse(preview.executionEnabled());
        assertFalse(preview.completeDependencyPlan());
        assertEquals(2, preview.categories().get("PRIVATE_TEMPLATES").recordCount());
        assertEquals(4, preview.categories().get("TASK_DEFINITIONS").recordCount());
        assertEquals(3, preview.categories().get("REWARD_DEFINITIONS").recordCount());
        assertEquals(5, preview.categories().get("TASK_EXECUTIONS").recordCount());
        assertEquals(6, preview.categories().get("REWARD_REQUESTS").recordCount());
        assertEquals(7, preview.categories().get("REWARD_PURCHASES").recordCount());
        assertEquals(8, preview.categories().get("POINT_LEDGER").recordCount());
        assertEquals(9, preview.categories().get("REPUTATION_LEDGER").recordCount());
        assertTrue(preview.categories().values().stream().noneMatch(
                WorkspaceDataResetPreviewController.ResetCategory::executable));
        assertTrue(preview.alwaysPreserved().contains("Other workspaces"));
        verify(db, never()).update(anyString(), any(Object[].class));
    }

    @Test void missingWorkspaceReturnsNotFoundBeforeInventory() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        when(db.queryForObject(contains("FROM families"), eq(Integer.class), eq(404L)))
                .thenReturn(0);
        assertThrows(ResponseStatusException.class,
                () -> new WorkspaceDataResetPreviewController(db).preview(404L));
        verify(db, times(1)).queryForObject(anyString(), eq(Integer.class), eq(404L));
        verify(db, never()).update(anyString(), any(Object[].class));
    }

    @Test void previewEndpointRequiresWorkspaceManagement() {
        var annotation = WorkspaceDataResetPreviewController.class.getAnnotation(
                org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(annotation);
        assertTrue(annotation.value().contains("canManageWorkspace"));
    }
}
