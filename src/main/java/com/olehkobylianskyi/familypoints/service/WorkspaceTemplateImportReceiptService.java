package com.olehkobylianskyi.familypoints.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olehkobylianskyi.familypoints.controller.WorkspaceTemplateController;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * One transaction: original client epoch -> family lock -> receipt lookup -> private
 * import -> immutable result receipt. Committed same-key replay returns the ORIGINAL
 * response, even if another subsequent import has changed the private catalog.
 */
@Service
public class WorkspaceTemplateImportReceiptService {
    private final JdbcTemplate db;
    private final WorkspaceEpochTransactionGuard epochGuard;
    private final WorkspaceTemplateController templates;
    private final ObjectMapper json = new ObjectMapper();

    public WorkspaceTemplateImportReceiptService(JdbcTemplate db,
            WorkspaceEpochTransactionGuard epochGuard, WorkspaceTemplateController templates) {
        this.db = db;
        this.epochGuard = epochGuard;
        this.templates = templates;
    }

    public record Result(UUID operationId, boolean replayed,
            WorkspaceTemplateController.ImportPreview importResult) {}
    private record Stored(String fingerprint, String resultJson) {}

    @Transactional
    public Result importOnce(long workspaceId, UUID operationId,
            WorkspaceTemplateController.ImportRequest request) {
        if (operationId == null || request == null || request.selectedCodes() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEMPLATE_IMPORT_INVALID_REQUEST");

        // Validate before touching the database. The ordinary import performs the
        // authoritative catalog validation again *inside* the locked transaction.
        List<String> codes = request.selectedCodes();
        if (codes.stream().anyMatch(code -> code == null || code.isBlank())
                || codes.stream().distinct().count() != codes.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEMPLATE_IMPORT_INVALID_SELECTION");

        String selected = serialize(codes);
        String fingerprint = sha256(selected);
        epochGuard.requireCurrentRequestEpoch(workspaceId);

        var existing = db.query("""
                SELECT request_sha256,response_json FROM workspace_template_import_receipts
                WHERE workspace_id=? AND operation_id=?
                """, (rs, i) -> new Stored(rs.getString(1),rs.getString(2)),
                workspaceId,operationId);
        if (!existing.isEmpty()) {
            if (!fingerprint.equals(existing.get(0).fingerprint()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "TEMPLATE_IMPORT_OPERATION_CONFLICT");
            return new Result(operationId,true,deserialize(existing.get(0).resultJson()));
        }

        var result = templates.importSelected(workspaceId,request);
        String response = serialize(result);
        db.update("""
                INSERT INTO workspace_template_import_receipts(
                    workspace_id,operation_id,request_sha256,selected_codes_json,response_json)
                VALUES (?,?,?,?,?)
                """,workspaceId,operationId,fingerprint,selected,response);
        return new Result(operationId,false,result);
    }

    @Transactional(readOnly=true)
    public Result lookup(long workspaceId, UUID operationId) {
        if (operationId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"TEMPLATE_IMPORT_INVALID_REQUEST");
        var rows=db.query("""
                SELECT response_json FROM workspace_template_import_receipts
                WHERE workspace_id=? AND operation_id=?
                """,(rs,i)->rs.getString(1),workspaceId,operationId);
        if(rows.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"TEMPLATE_IMPORT_RECEIPT_NOT_FOUND");
        return new Result(operationId,true,deserialize(rows.get(0)));
    }

    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Receipt serialization failed",e); }
    }
    private WorkspaceTemplateController.ImportPreview deserialize(String value) {
        try { return json.readValue(value,WorkspaceTemplateController.ImportPreview.class); }
        catch(JsonProcessingException e) { throw new IllegalStateException("Invalid persisted import receipt",e); }
    }
    private static String sha256(String value) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
