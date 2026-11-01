package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Explicit and bounded wizard finalization: private catalog import + setup completion.
 * Does not manufacture task/reward definitions, group hierarchies or ledger mutations.
 * A single family lock protects the complete operation and durable replay response.
 */
@Service
public class WorkspaceSetupFinalizeService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epoch;
    private final WorkspaceSetupService setup;
    private final WorkspaceTemplateImportReceiptService imports;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public WorkspaceSetupFinalizeService(JdbcTemplate db, WorkspaceEpochTransactionGuard epoch,
            WorkspaceSetupService setup, WorkspaceTemplateImportReceiptService imports) {
        this.db=db;this.epoch=epoch;this.setup=setup;this.imports=imports;
    }

    public record Request(Long expectedRevision, List<String> selectedCatalogCodes) {}
    public record Result(UUID operationId, boolean replayed, long workspaceId,
                         long dataEpoch, long revision, String setupStatus,
                         WorkspaceTemplateController.ImportPreview importResult) {}
    private record Stored(String fingerprint,String response) {}

    @Transactional
    public Result finalizeWizard(long workspaceId,UUID key,Request request) {
        if(key==null||request==null||request.expectedRevision()==null||
                request.selectedCatalogCodes()==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"SETUP_FINALIZE_INVALID");
        String hash=fingerprint(request);
        epoch.requireCurrentRequestEpoch(workspaceId); // serialization fence
        var rows=db.query("""
                SELECT request_sha256,result_json FROM workspace_setup_finalize_receipts
                WHERE workspace_id=? AND operation_id=?
                """,(rs,i)->new Stored(rs.getString(1),rs.getString(2)),workspaceId,key);
        if(!rows.isEmpty()) {
            var row=rows.get(0);
            if(!row.fingerprint().equals(hash))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_FINALIZE_KEY_CONFLICT");
            try {
                Result original=json.readValue(row.response(),Result.class);
                return new Result(key,true,original.workspaceId(),original.dataEpoch(),
                        original.revision(),original.setupStatus(),original.importResult());
            } catch(Exception e) {throw new IllegalStateException("Invalid finalize receipt",e);}
        }

        var state=setup.current(workspaceId); // feature gate and epoch-scoped row
        if(state.revision()!=request.expectedRevision())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_REVISION_STALE");
        if("COMPLETED".equals(state.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_ALREADY_COMPLETED");

        // Validate final review and all steps BEFORE a domain import. The ordinary
        // setup.complete() repeats the checks and writes within the same transaction.
        if(!state.steps().keySet().containsAll(WorkspaceSetupService.STEPS) ||
                state.steps().values().stream().anyMatch(v->!List.of("COMPLETE","SKIPPED").contains(v)))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"SETUP_STEPS_INCOMPLETE");

        var imported=imports.importOnce(workspaceId,key,
                new WorkspaceTemplateController.ImportRequest(request.selectedCatalogCodes()));
        var completed=setup.complete(workspaceId,request.expectedRevision());
        var result=new Result(key,false,workspaceId,completed.dataEpoch(),
                completed.revision(),completed.status(),imported.importResult());
        try {
            db.update("""
                    INSERT INTO workspace_setup_finalize_receipts
                       (workspace_id,operation_id,data_epoch,expected_revision,request_sha256,result_json)
                    VALUES (?,?,?,?,?,?)
                    """,workspaceId,key,state.dataEpoch(),request.expectedRevision(),
                    hash,json.writeValueAsString(result));
        } catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Cannot persist finalize receipt",e);
        }
        return result;
    }

    @Transactional(readOnly=true)
    public Result receipt(long workspaceId,UUID key) {
        if(key==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"SETUP_FINALIZE_INVALID");
        setup.current(workspaceId); // feature-flag and fresh-epoch check
        var rows=db.query("""
                SELECT result_json FROM workspace_setup_finalize_receipts
                WHERE workspace_id=? AND operation_id=? AND data_epoch=(
                   SELECT data_epoch FROM families WHERE id=?)
                """,(rs,i)->rs.getString(1),workspaceId,key,workspaceId);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"SETUP_FINALIZE_NOT_FOUND");
        try {
            var previous=json.readValue(rows.get(0),Result.class);
            return new Result(key,true,previous.workspaceId(),previous.dataEpoch(),
                    previous.revision(),previous.setupStatus(),previous.importResult());
        } catch(Exception e) {throw new IllegalStateException("Invalid finalize receipt",e);}
    }

    private String fingerprint(Request request) {
        try {
            byte[] data=json.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        }catch(Exception e){throw new IllegalStateException("Cannot fingerprint finalize request",e);}
    }
}
