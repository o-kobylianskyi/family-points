package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.service.TemplateCatalogService;
import com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.RewardDefinitionRepository;
import com.olehkobylianskyi.familypoints.entity.TaskDefinition;
import com.olehkobylianskyi.familypoints.entity.RewardDefinition;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceTemplateControllerTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final TaskDefinitionRepository tasks = mock(TaskDefinitionRepository.class);
    private final RewardDefinitionRepository rewards = mock(RewardDefinitionRepository.class);
    private final WorkspaceTemplateController controller =
            new WorkspaceTemplateController(db, new TemplateCatalogService(),
                    tasks, rewards,
                    mock(TaskController.class), mock(RewardController.class));

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
    @Test void cannotSaveForeignTaskAsTemplate() {
        when(tasks.findByIdAndWorkspaceId(55L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> controller.saveFromTask(1L, 55L));
        verifyNoInteractions(db);
    }

    @Test void cannotSaveForeignRewardAsTemplate() {
        when(rewards.findByIdAndWorkspaceId(55L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> controller.saveFromReward(1L, 55L));
        verifyNoInteractions(db);
    }

    @Test void saveExistingTaskCopiesDefinitionOnly() {
        TaskDefinition task = mock(TaskDefinition.class);
        when(tasks.findByIdAndWorkspaceId(7L, 1L)).thenReturn(Optional.of(task));
        when(task.getTitle()).thenReturn("Cleaning");
        when(task.getDescription()).thenReturn("Weekly");
        controller.saveFromTask(1L, 7L);
        verify(db).update(contains("INSERT INTO workspace_private_templates"),
                eq(1L), eq("TASK"), eq("Cleaning"), argThat((String raw) ->
                    raw.contains("Cleaning") && raw.contains("Weekly")
                    && !raw.contains("assignedMemberId") && !raw.contains("ledger")));
    }

    @Test void saveExistingRewardCopiesDefinitionOnly() {
        RewardDefinition reward = mock(RewardDefinition.class);
        var currency = mock(com.olehkobylianskyi.familypoints.entity.PointType.class);
        when(rewards.findByIdAndWorkspaceId(9L, 1L)).thenReturn(Optional.of(reward));
        when(reward.getTitle()).thenReturn("Game time");
        when(reward.getPointType()).thenReturn(currency);
        when(currency.getCode()).thenReturn("POINTS");
        when(reward.getRewardKind()).thenReturn(com.olehkobylianskyi.familypoints.entity.RewardKind.STANDARD);
        when(reward.getAcquisitionMode()).thenReturn(com.olehkobylianskyi.familypoints.entity.RewardAcquisitionMode.DIRECT);
        when(reward.getAvailabilityMode()).thenReturn(com.olehkobylianskyi.familypoints.entity.RewardAvailabilityMode.ALWAYS);
        when(reward.getWeeklyWindows()).thenReturn(List.of());
        controller.saveFromReward(1L, 9L);
        verify(db).update(contains("INSERT INTO workspace_private_templates"),
                eq(1L), eq("REWARD"), eq("Game time"), argThat((String raw) ->
                    raw.contains("POINTS") && !raw.contains("purchaseId") && !raw.contains("memberId")));
    }
    @Test void instantiatePreviewDoesNotWriteAndRequiresScopedTemplate() {
        when(db.query(contains("SELECT kind,title,definition_json"), any(org.springframework.jdbc.core.RowMapper.class),
                eq(99L),eq(1L))).thenReturn(List.of());
        assertThrows(ResponseStatusException.class,()->controller.instantiatePreview(1L,99L));
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void previewPayloadIndicatesAssignmentAndCurrencySelection() {
        when(db.query(contains("SELECT kind,title,definition_json"), any(org.springframework.jdbc.core.RowMapper.class),
                eq(7L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"TASK","Walk",
                        "{\"source\":\"TASK\",\"rewardCurrencyCode\":\"POINTS\"}"}));
        var draft=controller.instantiatePreview(1L,7L);
        assertEquals("Walk",draft.title());
        assertTrue(draft.requiresAssignment());
        assertTrue(draft.requiresCurrencySelection());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void missingTemplateCannotBeActivated() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class), eq(999L), eq(1L)))
                .thenReturn(List.of());
        assertThrows(ResponseStatusException.class,
                () -> controller.activateTask(1L,999L,java.util.UUID.randomUUID(),
                        new com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest()));
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void rewardCannotBeActivatedAsTask() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class), eq(5L), eq(1L)))
                .thenReturn(List.of("REWARD"));
        assertThrows(ResponseStatusException.class,
                () -> controller.activateTask(1L,5L,java.util.UUID.randomUUID(),
                        new com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest()));
        verify(db,never()).update(anyString(),any(Object[].class));
    }
}
