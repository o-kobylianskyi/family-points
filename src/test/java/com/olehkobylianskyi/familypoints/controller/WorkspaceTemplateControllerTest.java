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

    @Test void creationDraftUsesTemplateTitleAndDropsWorkspaceSpecificIds() {
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class), eq(7L), eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"TASK","Walk",
                        "{\"description\":\"Outside\",\"rewardAmount\":20,\"assignedMemberId\":999,\"rewardPointTypeId\":888}"}));
        when(db.query(contains("SELECT id FROM point_types"), any(org.springframework.jdbc.core.RowMapper.class),
                eq(1L),eq("POINTS"))).thenReturn(List.of(23L));
        var draft=controller.creationDraft(1L,7L);
        assertEquals("Walk",draft.path("request").path("title").asText());
        assertEquals(20,draft.path("request").path("rewardAmount").asInt());
        assertTrue(draft.path("request").path("assignedMemberId").isNull());
        assertTrue(draft.path("request").path("rewardPointTypeId").isNull());
        assertTrue(draft.path("request").path("startDate").isNull());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void creationDraftResolvesCurrencyOnlyInTargetWorkspace() {
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(5L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Game time",
                        "{\"currencyCode\":\"POINTS\",\"priceAmount\":50}"}));
        when(db.query(contains("SELECT id FROM point_types"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(1L),eq("POINTS")))
                .thenReturn(List.of(42L));
        var draft=controller.creationDraft(1L,5L);
        assertEquals(42L,draft.path("request").path("pointTypeId").asLong());
        assertEquals(50,draft.path("request").path("priceAmount").asInt());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void activationRejectsIncompleteTaskBeforeClaimingIdempotency() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(10L),eq(1L)))
                .thenReturn(List.of("TASK"));
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(10L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"TASK","Walk","{}"}));
        assertThrows(ResponseStatusException.class, () -> controller.activateTask(1L,10L,
                java.util.UUID.randomUUID(),
                new com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest()));
        verify(db,never()).update(contains("template_activation_requests"),any(Object[].class));
    }

    @Test void activationRejectsRewardWithoutCurrencyBeforeClaim() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(11L),eq(1L)))
                .thenReturn(List.of("REWARD"));
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(11L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Prize","{}"}));
        var request=new com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest();
        request.setTitle("Prize");
        assertThrows(ResponseStatusException.class, () -> controller.activateReward(1L,11L,
                java.util.UUID.randomUUID(),request));
        verify(db,never()).update(contains("template_activation_requests"),any(Object[].class));
    }

    @Test void activationRejectsCurrencyFromAnotherFamilyBeforeClaim() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(12L),eq(1L)))
                .thenReturn(List.of("REWARD"));
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(12L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Prize","{}"}));
        when(db.queryForObject(contains("FROM point_types"),eq(Integer.class),
                eq(998L),eq(1L))).thenReturn(0);
        var request=new com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest();
        request.setTitle("Prize");
        request.setPointTypeId(998L);
        assertThrows(ResponseStatusException.class, () -> controller.activateReward(1L,12L,
                java.util.UUID.randomUUID(),request));
        verify(db,never()).update(contains("template_activation_requests"),any(Object[].class));
    }

    @Test void activationRejectsMissingAssigneeBeforeClaim() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(15L),eq(1L)))
                .thenReturn(List.of("TASK"));
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(15L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"TASK","Walk","{}"}));
        var request=new com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest();
        request.setTitle("Walk");
        request.setRecurrenceType(com.olehkobylianskyi.familypoints.entity.TaskRecurrenceType.DAILY);
        request.setStartDate(java.time.LocalDate.of(2026,10,27));
        assertThrows(ResponseStatusException.class,()->controller.activateTask(1L,15L,
                java.util.UUID.randomUUID(),request));
        verify(db,never()).update(contains("template_activation_requests"),any(Object[].class));
    }

    @Test void activationRequiresGroupForGroupSharedPolicy() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(18L),eq(1L)))
                .thenReturn(List.of("TASK"));
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(18L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"TASK","Cleanup","{}"}));
        var req=new com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest();
        req.setTitle("Cleanup");
        req.setStartDate(java.time.LocalDate.of(2026,10,27));
        req.setRecurrenceType(com.olehkobylianskyi.familypoints.entity.TaskRecurrenceType.ONCE);
        req.setAssignmentPolicy(com.olehkobylianskyi.familypoints.entity.AssignmentPolicy.GROUP_SHARED);
        assertThrows(ResponseStatusException.class,()->controller.activateTask(1L,18L,
                java.util.UUID.randomUUID(),req));
        verify(db,never()).update(contains("template_activation_requests"),any(Object[].class));
    }

    @Test void importedCatalogDefinitionProducesUsableTaskDraft() {
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(24L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"TASK","Brush teeth",
                        """
                        {"code":"fp.task.hygiene.001","kind":"TASK",
                         "definition":{"recurrenceType":"DAILY","mandatory":false,"rewardAmount":10}}
                        """}));
        var request=controller.creationDraft(1L,24L).path("request");
        assertEquals("DAILY",request.path("recurrenceType").asText());
        assertEquals(10,request.path("rewardAmount").asInt());
        assertTrue(request.path("assignedMemberId").isNull());
    }

    @Test void designerTimeRewardDurationMapsToCreationDto() {
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(42L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Cartoons",
                        """
                        {"code":"fp.reward.time.002","kind":"REWARD",
                         "definition":{"rewardKind":"TIME","durationMinutes":30,
                                       "priceAmount":null,"currencyCode":null}}
                        """}));
        var request=controller.creationDraft(1L,42L).path("request");
        assertEquals("TIME",request.path("rewardKind").asText());
        assertEquals(30,request.path("defaultDurationMinutes").asInt());
        assertTrue(request.path("pointTypeId").isNull());
    }

    @Test void explicitDurationTakesPriorityOverLegacyDesignerDuration() {
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(43L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Timed",
                        """
                        {"definition":{"rewardKind":"TIME","durationMinutes":15,
                         "defaultDurationMinutes":45}}
                        """}));
        assertEquals(45,controller.creationDraft(1L,43L).path("request")
                .path("defaultDurationMinutes").asInt());
    }

    @Test void foreignFamilyTemplateCannotBeActivatedEvenWithValidLookingRequest() {
        // The template ID is valid elsewhere, but the scoped lookup must fail.
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(301L),eq(2L)))
                .thenReturn(List.of());
        var req = new com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest();
        req.setTitle("Foreign reward");
        req.setPointTypeId(12L);
        assertThrows(ResponseStatusException.class, () -> controller.activateReward(
                2L,301L,java.util.UUID.randomUUID(),req));
        verify(db,never()).update(contains("template_activation_requests"),any(Object[].class));
        verify(db,never()).queryForObject(contains("FROM point_types"),
                eq(Integer.class),any(Object[].class));
    }

    @Test void taskCannotBeActivatedThroughRewardEndpoint() {
        when(db.query(contains("SELECT kind FROM workspace_private_templates"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(302L),eq(1L)))
                .thenReturn(List.of("TASK"));
        var req = new com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest();
        req.setTitle("Wrong kind");
        req.setPointTypeId(12L);
        assertThrows(ResponseStatusException.class, () -> controller.activateReward(
                1L,302L,java.util.UUID.randomUUID(),req));
        verify(db,never()).update(contains("template_activation_requests"),any(Object[].class));
    }

    @Test void activationEndpointsDeclareBothWorkspaceAndActionPermissions() throws Exception {
        var type=WorkspaceTemplateController.class;
        var base=type.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(base);
        assertTrue(base.value().contains("canManageWorkspace"));
        var task=type.getMethod("activateTask",Long.class,Long.class,java.util.UUID.class,
                com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest.class);
        var reward=type.getMethod("activateReward",Long.class,Long.class,java.util.UUID.class,
                com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest.class);
        var taskRule=task.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        var rewardRule=reward.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(taskRule);
        assertNotNull(rewardRule);
        assertTrue(taskRule.value().contains("canManageWorkspace"));
        assertTrue(taskRule.value().contains("CREATE_TASKS"));
        assertTrue(rewardRule.value().contains("canManageWorkspace"));
        assertTrue(rewardRule.value().contains("MANAGE_REWARDS"));
        assertNotNull(task.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertNotNull(reward.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test void creationDraftExposesSuggestionWithoutApplyingPriceOrPoints() {
        var catalog = new TemplateCatalogService();
        var economy = new com.olehkobylianskyi.familypoints.service.TemplateEconomySuggestionsService(catalog);
        var scopedController = new WorkspaceTemplateController(db, catalog, tasks, rewards,
                mock(TaskController.class), mock(RewardController.class), economy);
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(71L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Cartoons",
                        """
                        {"code":"fp.reward.time.002","kind":"REWARD",
                         "definition":{"rewardKind":"TIME","durationMinutes":30,
                                       "priceAmount":null,"currencyCode":null}}
                        """}));
        var draft = scopedController.creationDraft(1L,71L);
        assertEquals(30,draft.path("request").path("defaultDurationMinutes").asInt());
        assertFalse(draft.path("request").has("priceAmount"));
        assertTrue(draft.path("request").path("pointTypeId").isNull());
        assertTrue(draft.path("economySuggestion").path("suggestedPricePoints").isNumber());
        assertTrue(draft.path("requiresConfirmation").asBoolean());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void nullSuggestionIsNotCoercedIntoFreeReward() {
        var catalog=new TemplateCatalogService();
        var economy=new com.olehkobylianskyi.familypoints.service.TemplateEconomySuggestionsService(catalog);
        var scopedController=new WorkspaceTemplateController(db,catalog,tasks,rewards,
                mock(TaskController.class),mock(RewardController.class),economy);
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(72L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Family time",
                        """
                        {"code":"fp.reward.privilege.013","kind":"REWARD",
                         "definition":{"rewardKind":"STANDARD","priceAmount":null,"currencyCode":null}}
                        """}));
        var draft=scopedController.creationDraft(1L,72L);
        assertFalse(draft.path("request").has("priceAmount"));
        assertTrue(draft.path("request").path("pointTypeId").isNull());
        assertTrue(draft.path("requiresConfirmation").asBoolean());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void explicitEconomyApplyRequiresActiveFamilyPointsAndDoesNotWrite() {
        var catalog=new TemplateCatalogService();
        var economy=new com.olehkobylianskyi.familypoints.service.TemplateEconomySuggestionsService(catalog);
        var scoped=new WorkspaceTemplateController(db,catalog,tasks,rewards,
                mock(TaskController.class),mock(RewardController.class),economy);
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(81L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Cartoons",
                        """
                        {"code":"fp.reward.time.002","definition":{"rewardKind":"TIME","durationMinutes":30}}
                        """}));
        when(db.query(contains("SELECT id FROM point_types WHERE family_id=? AND code='POINTS'"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(1L)))
                .thenReturn(List.of(7L));
        var draft=scoped.applyEconomySuggestion(1L,81L);
        assertEquals(45,draft.path("request").path("priceAmount").asInt());
        assertEquals(7L,draft.path("request").path("pointTypeId").asLong());
        assertTrue(draft.path("requiresConfirmation").asBoolean());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void missingFamilyCurrencyCannotApplySuggestedRewardPrice() {
        var catalog=new TemplateCatalogService();
        var economy=new com.olehkobylianskyi.familypoints.service.TemplateEconomySuggestionsService(catalog);
        var scoped=new WorkspaceTemplateController(db,catalog,tasks,rewards,
                mock(TaskController.class),mock(RewardController.class),economy);
        when(db.query(contains("SELECT kind,title,definition_json"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(82L),eq(1L)))
                .thenReturn(List.<String[]>of(new String[]{"REWARD","Cartoons",
                        """
                        {"code":"fp.reward.time.002","definition":{"rewardKind":"TIME","durationMinutes":30}}
                        """}));
        when(db.query(contains("SELECT id FROM point_types WHERE family_id=? AND code='POINTS'"),
                any(org.springframework.jdbc.core.RowMapper.class),eq(1L)))
                .thenReturn(List.of());
        assertThrows(ResponseStatusException.class,()->scoped.applyEconomySuggestion(1L,82L));
        verify(db,never()).update(anyString(),any(Object[].class));
    }
}
