package com.olehkobylianskyi.familypoints.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.olehkobylianskyi.familypoints.repository.TaskDefinitionRepository;
import com.olehkobylianskyi.familypoints.repository.RewardDefinitionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.olehkobylianskyi.familypoints.service.TemplateCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** Private definitions only: never creates active tasks/rewards, purchases or ledger entries. */
@RestController
@RequestMapping("/workspaces/{workspaceId}/templates")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceTemplateController {
    private final JdbcTemplate db;
    private final TemplateCatalogService catalog;
    private final TaskDefinitionRepository taskDefinitions;
    private final RewardDefinitionRepository rewardDefinitions;
    private final ObjectMapper json = new ObjectMapper();
    private final TaskController taskController;
    private final RewardController rewardController;

    public WorkspaceTemplateController(JdbcTemplate db, TemplateCatalogService catalog,
                                       TaskDefinitionRepository taskDefinitions,
                                       RewardDefinitionRepository rewardDefinitions,
                                       TaskController taskController, RewardController rewardController) {
        this.db = db;
        this.catalog = catalog;
        this.taskDefinitions = taskDefinitions;
        this.rewardDefinitions = rewardDefinitions;
        this.taskController = taskController;
        this.rewardController = rewardController;
    }

    public record PrivateTemplate(Long id, String kind, String title, String definitionJson,
                                  String catalogCode, Integer catalogVersion) {}
    public record TemplateRequest(@NotBlank @Size(max = 150) String title, @NotBlank String definitionJson,
                                  @NotBlank String kind) {}
    public record ImportRequest(@NotNull List<@NotBlank String> selectedCodes) {}
    public record InstantiateDraft(Long templateId, String kind, String title, JsonNode definition,
                                   boolean requiresAssignment, boolean requiresCurrencySelection) {}
    public record ImportPreview(List<String> selectedCodes, List<String> alreadyImported,
                                List<String> toImport) {}


    /**
     * Snapshot configuration only; references to participants, history, purchases
     * and ledgers are intentionally excluded from portable private templates.
     */
    @PostMapping("/from-tasks/{taskId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void saveFromTask(@PathVariable Long workspaceId, @PathVariable Long taskId) {
        var task = taskDefinitions.findByIdAndWorkspaceId(taskId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        ObjectNode data = json.createObjectNode();
        data.put("source", "TASK");
        data.put("title", task.getTitle());
        data.put("description", task.getDescription());
        data.put("mandatory", task.isMandatory());
        data.put("recurrenceType", task.getRecurrenceType() == null ? null : task.getRecurrenceType().name());
        data.put("recurrenceDayOfWeek", task.getRecurrenceDayOfWeek());
        data.put("recurrenceDayOfMonth", task.getRecurrenceDayOfMonth());
        data.put("dueTime", task.getDueTime() == null ? null : task.getDueTime().toString());
        data.put("rewardCurrencyCode", task.getRewardPointType() == null ? null : task.getRewardPointType().getCode());
        data.put("rewardAmount", task.getRewardAmount());
        data.put("penaltyCurrencyCode", task.getPenaltyPointType() == null ? null : task.getPenaltyPointType().getCode());
        data.put("penaltyAmount", task.getPenaltyAmount());
        data.put("rewardReputation", task.getRewardReputationAmount());
        data.put("penaltyReputation", task.getPenaltyReputationAmount());
        create(workspaceId, new TemplateRequest(task.getTitle(), data.toString(), "TASK"));
    }

    @PostMapping("/from-rewards/{rewardId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void saveFromReward(@PathVariable Long workspaceId, @PathVariable Long rewardId) {
        var reward = rewardDefinitions.findByIdAndWorkspaceId(rewardId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reward not found"));
        ObjectNode data = json.createObjectNode();
        data.put("source", "REWARD");
        data.put("title", reward.getTitle());
        data.put("description", reward.getDescription());
        data.put("currencyCode", reward.getPointType().getCode());
        data.put("priceAmount", reward.getPriceAmount());
        data.put("rewardKind", reward.getRewardKind().name());
        data.put("durationMinutes", reward.getDefaultDurationMinutes());
        data.put("acquisitionMode", reward.getAcquisitionMode().name());
        data.put("requiresApproval", reward.isRequiresApproval());
        data.put("minimumReputation", reward.getMinimumReputation());
        data.put("cooldownSeconds", reward.getCooldownSeconds());
        data.put("memberTotalLimit", reward.getMemberTotalLimit());
        data.put("memberDailyLimit", reward.getMemberDailyLimit());
        data.put("memberWeeklyLimit", reward.getMemberWeeklyLimit());
        data.put("workspaceTotalLimit", reward.getWorkspaceTotalLimit());
        data.put("workspaceDailyLimit", reward.getWorkspaceDailyLimit());
        data.put("workspaceWeeklyLimit", reward.getWorkspaceWeeklyLimit());
        data.put("extraRequestAllowed", reward.isExtraRequestAllowed());
        data.put("availabilityMode", reward.getAvailabilityMode().name());
        data.put("availabilityStartsAt", reward.getAvailabilityStartsAt() == null ? null : reward.getAvailabilityStartsAt().toString());
        data.put("availabilityEndsAt", reward.getAvailabilityEndsAt() == null ? null : reward.getAvailabilityEndsAt().toString());
        // Weekly windows are not copied until their exact portable DTO is implemented.
        if (!reward.getWeeklyWindows().isEmpty())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Weekly availability templates are not yet supported");
        create(workspaceId, new TemplateRequest(reward.getTitle(), data.toString(), "REWARD"));
    }

    @GetMapping
    public List<PrivateTemplate> list(@PathVariable Long workspaceId) {
        return db.query("""
                SELECT id,kind,title,definition_json,catalog_code,catalog_version
                FROM workspace_private_templates WHERE workspace_id=? ORDER BY id
                """, (rs,i)->new PrivateTemplate(rs.getLong(1),rs.getString(2),rs.getString(3),
                rs.getString(4),rs.getString(5),(Integer)rs.getObject(6)),workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@PathVariable Long workspaceId,@Valid @RequestBody TemplateRequest request) {
        checkKind(request.kind());
        validateDefinition(request.definitionJson());
        db.update("""
                INSERT INTO workspace_private_templates(workspace_id,kind,title,definition_json)
                VALUES(?,?,?,?)
                """, workspaceId,request.kind(),request.title().trim(),request.definitionJson());
    }

    @PutMapping("/{templateId}")
    public void update(@PathVariable Long workspaceId,@PathVariable Long templateId,
                       @Valid @RequestBody TemplateRequest request) {
        checkKind(request.kind());
        validateDefinition(request.definitionJson());
        int count=db.update("""
                UPDATE workspace_private_templates SET kind=?,title=?,definition_json=?,updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND workspace_id=?
                """,request.kind(),request.title().trim(),request.definitionJson(),templateId,workspaceId);
        if(count!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Template not found");
    }

    @DeleteMapping("/{templateId}")
    public void delete(@PathVariable Long workspaceId,@PathVariable Long templateId) {
        int count=db.update("DELETE FROM workspace_private_templates WHERE id=? AND workspace_id=?",
                templateId,workspaceId);
        if(count!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Template not found");
    }


    /**
     * Explicit-use preparation endpoint. No executable object is created here.
     * The client must choose actors/currencies and submit through the existing
     * authorized task/reward creation flow after reviewing the configuration.
     */
    @GetMapping("/{templateId}/instantiate-preview")
    public InstantiateDraft instantiatePreview(@PathVariable Long workspaceId,
                                               @PathVariable Long templateId) {
        var rows = db.query("""
                SELECT kind,title,definition_json FROM workspace_private_templates
                WHERE id=? AND workspace_id=?
                """, (rs,i) -> new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},
                templateId,workspaceId);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Template not found");
        String[] row=rows.get(0);
        try {
            JsonNode payload=json.readTree(row[2]);
            if(payload == null || !payload.isObject())
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid template payload");
            // Portable templates store currency codes rather than tenant-specific IDs.
            // Those IDs and assigned members must be chosen/resolved per workspace at activation.
            return new InstantiateDraft(templateId,row[0],row[1],payload,
                    "TASK".equals(row[0]),
                    "REWARD".equals(row[0]) || payload.hasNonNull("rewardCurrencyCode")
                            || payload.hasNonNull("penaltyCurrencyCode"));
        } catch(JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid template payload");
        }
    }


    /**
     * Separate explicit activation confirmation. Reuses the established creation
     * controllers (and their service validators) rather than writing SQL into
     * task/reward tables. Caller supplies the final, editable creation request.
     * No activation occurs during catalog or private-library imports.
     */
    @PostMapping("/{templateId}/activate-task")
    @PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId) and hasAuthority('CREATE_TASKS')")
    @Transactional
    public org.springframework.http.ResponseEntity<com.olehkobylianskyi.familypoints.dto.TaskDefinitionResponse>
    activateTask(@PathVariable Long workspaceId, @PathVariable Long templateId,
                 @RequestHeader("Idempotency-Key") java.util.UUID operationId,
                 @Valid @RequestBody com.olehkobylianskyi.familypoints.dto.TaskDefinitionCreateRequest request) {
        requireKind(workspaceId, templateId, "TASK");
        claimActivation(workspaceId,templateId,"TASK",operationId);
        return taskController.createDefinition(workspaceId, request);
    }

    @PostMapping("/{templateId}/activate-reward")
    @PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId) and hasAuthority('MANAGE_REWARDS')")
    @Transactional
    public com.olehkobylianskyi.familypoints.dto.RewardDefinitionResponse
    activateReward(@PathVariable Long workspaceId, @PathVariable Long templateId,
                   @RequestHeader("Idempotency-Key") java.util.UUID operationId,
                   @Valid @RequestBody com.olehkobylianskyi.familypoints.dto.RewardDefinitionRequest request) {
        requireKind(workspaceId, templateId, "REWARD");
        claimActivation(workspaceId,templateId,"REWARD",operationId);
        return rewardController.createReward(workspaceId, request);
    }

    private void claimActivation(Long workspaceId,Long templateId,String kind,java.util.UUID operationId) {
        if(operationId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key UUID required");
        int claimed = db.update("""
                INSERT INTO template_activation_requests(workspace_id,operation_id,template_id,kind)
                VALUES(?,?,?,?) ON CONFLICT (workspace_id,operation_id) DO NOTHING
                """,workspaceId,operationId,templateId,kind);
        if(claimed != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Activation already submitted; refresh before retrying");
    }

    private void requireKind(Long workspaceId, Long templateId, String expected) {
        var kinds = db.query("SELECT kind FROM workspace_private_templates WHERE id=? AND workspace_id=?",
                (rs,i)->rs.getString(1),templateId,workspaceId);
        if (kinds.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        if (!expected.equals(kinds.get(0)))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Template kind mismatch");
    }

    @PostMapping("/imports/preview")
    public ImportPreview preview(@PathVariable Long workspaceId,@Valid @RequestBody ImportRequest request) {
        List<JsonNode> templates=resolveSelection(request.selectedCodes());
        List<String> existing=new ArrayList<>(),missing=new ArrayList<>();
        for(JsonNode t:templates){
            String code=t.path("code").asText();
            int count=db.queryForObject("""
                    SELECT count(*) FROM workspace_private_templates
                    WHERE workspace_id=? AND catalog_code=?
                    """,Integer.class,workspaceId,code);
            (count>0?existing:missing).add(code);
        }
        return new ImportPreview(templates.stream().map(t->t.path("code").asText()).toList(),existing,missing);
    }

    @PostMapping("/imports")
    @Transactional
    public ImportPreview importSelected(@PathVariable Long workspaceId,
                                        @Valid @RequestBody ImportRequest request) {
        List<JsonNode> templates=resolveSelection(request.selectedCodes());
        ImportPreview result=preview(workspaceId,request);
        int version=catalog.getCatalog().path("version").asInt();
        for(JsonNode t:templates){
            db.update("""
                    INSERT INTO workspace_private_templates(
                        workspace_id,kind,title,definition_json,catalog_code,catalog_version)
                    VALUES(?,?,?,?,?,?)
                    ON CONFLICT (workspace_id,catalog_code) WHERE catalog_code IS NOT NULL DO NOTHING
                    """,workspaceId,t.path("kind").asText(),t.path("title").path("uk").asText(),
                    t.toString(),t.path("code").asText(),version);
        }
        return result;
    }

    private List<JsonNode> resolveSelection(List<String> codes){
        if(codes.size()>100 || codes.stream().distinct().count()!=codes.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid selection");
        JsonNode all=catalog.getCatalog().path("templates");
        List<JsonNode> out=new ArrayList<>();
        for(String code:codes){
            JsonNode found=null;
            for(JsonNode item:all) if(code.equals(item.path("code").asText())) {found=item;break;}
            if(found==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown template code");
            out.add(found);
        }
        return out;
    }

    private void validateDefinition(String raw) {
        if (raw.length() > 30000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template too large");
        try {
            JsonNode node = json.readTree(raw);
            if (node == null || !node.isObject())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template definition must be a JSON object");
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed template JSON");
        }
    }

    private static void checkKind(String kind) {
        if(!List.of("TASK","REWARD","EVENT").contains(kind))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported template kind");
    }
}
