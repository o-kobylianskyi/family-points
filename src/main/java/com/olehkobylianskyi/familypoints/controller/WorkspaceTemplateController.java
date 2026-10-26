package com.olehkobylianskyi.familypoints.controller;

import com.fasterxml.jackson.databind.JsonNode;
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

    public WorkspaceTemplateController(JdbcTemplate db, TemplateCatalogService catalog) {
        this.db = db;
        this.catalog = catalog;
    }

    public record PrivateTemplate(Long id, String kind, String title, String definitionJson,
                                  String catalogCode, Integer catalogVersion) {}
    public record TemplateRequest(@NotBlank @Size(max = 150) String title, @NotBlank String definitionJson,
                                  @NotBlank String kind) {}
    public record ImportRequest(@NotNull List<@NotBlank String> selectedCodes) {}
    public record ImportPreview(List<String> selectedCodes, List<String> alreadyImported,
                                List<String> toImport) {}

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
        if (request.definitionJson().length() > 30000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Template too large");
        db.update("""
                INSERT INTO workspace_private_templates(workspace_id,kind,title,definition_json)
                VALUES(?,?,?,?)
                """, workspaceId,request.kind(),request.title().trim(),request.definitionJson());
    }

    @PutMapping("/{templateId}")
    public void update(@PathVariable Long workspaceId,@PathVariable Long templateId,
                       @Valid @RequestBody TemplateRequest request) {
        checkKind(request.kind());
        if (request.definitionJson().length() > 30000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Template too large");
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

    private static void checkKind(String kind) {
        if(!List.of("TASK","REWARD","EVENT").contains(kind))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported template kind");
    }
}
