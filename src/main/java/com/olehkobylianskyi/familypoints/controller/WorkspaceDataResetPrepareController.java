package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * BKL-002: prepare and persist an auditable reset proposal ONLY.
 * This controller deliberately exposes no confirmation or delete operation.
 */
@RestController
@RequestMapping("/workspaces/{workspaceId}/data-reset")
@PreAuthorize("@workspaceSecurity.canManageWorkspace(#workspaceId)")
public class WorkspaceDataResetPrepareController {
    private final JdbcTemplate db;
    private final CurrentUserService current;
    private final WorkspaceDataResetPreviewController preview;

    public WorkspaceDataResetPrepareController(JdbcTemplate db, CurrentUserService current,
                                                WorkspaceDataResetPreviewController preview) {
        this.db=db; this.current=current; this.preview=preview;
    }

    public record PrepareRequest(UUID operationId, Set<String> categories) {}
    public record PreparedReset(UUID operationId, Long workspaceId, List<String> categories,
                                String snapshotDigest, String status, boolean executable) {}

    @PostMapping("/prepare")
    @Transactional
    public PreparedReset prepare(@PathVariable Long workspaceId, @RequestBody PrepareRequest request) {
        if (request == null || request.operationId() == null || request.categories() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operation ID and categories required");
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong workspace");
        long memberId=current.getCurrentMemberId();
        var plan=preview.plan(workspaceId,
                new WorkspaceDataResetPreviewController.ResetPlanRequest(request.categories()));
        var selected=plan.selectedCategories().stream().sorted().toList();
        var inventory=preview.preview(workspaceId);
        StringBuilder fingerprint=new StringBuilder("reset-v1|").append(workspaceId).append('|');
        for (String key:selected) {
            var category=inventory.categories().get(key);
            fingerprint.append(key).append('=').append(category.recordCount()).append(';');
        }
        String digest=sha256(fingerprint.toString());
        // Chosen category names are from the server-side enum allowlist.
        String json=selected.stream().map(key -> "\"" + key + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        db.update("""
                INSERT INTO workspace_reset_operations
                (workspace_id, operation_id, requested_by_member_id, selected_categories_json, snapshot_digest)
                VALUES (?,?::uuid,?,?,?)
                ON CONFLICT (workspace_id,operation_id) DO NOTHING
                """,workspaceId,request.operationId().toString(),memberId,json,digest);
        var stored=db.queryForMap("""
                SELECT requested_by_member_id,selected_categories_json,snapshot_digest,status
                FROM workspace_reset_operations
                WHERE workspace_id=? AND operation_id=?::uuid
                """,workspaceId,request.operationId().toString());
        if (!java.util.Objects.equals(((Number)stored.get("requested_by_member_id")).longValue(),memberId)
                || !json.equals(stored.get("selected_categories_json"))
                || !digest.equals(stored.get("snapshot_digest")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reset operation replay differs from original");
        return new PreparedReset(request.operationId(),workspaceId,selected,digest,
                (String)stored.get("status"),false);
    }

    /**
     * Records an explicit, scoped acknowledgement. This is NOT reauthentication,
     * nor permission to execute deletion; executor remains intentionally absent.
     */
    public record ConfirmRequest(UUID operationId, String snapshotDigest, String phrase) {}
    public record ConfirmedReset(UUID operationId, String status, boolean executable) {}

    @PostMapping("/confirm")
    @Transactional
    public ConfirmedReset confirm(@PathVariable Long workspaceId, @RequestBody ConfirmRequest request) {
        if (request == null || request.operationId() == null || request.snapshotDigest() == null
                || !("RESET " + workspaceId).equals(request.phrase()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Explicit reset phrase and digest required");
        if (!current.belongsToWorkspace(workspaceId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong workspace");
        long memberId = current.getCurrentMemberId();
        // Lock the exact workspace operation; another member cannot confirm it.
        var records = db.queryForList("""
                SELECT requested_by_member_id, snapshot_digest, status
                FROM workspace_reset_operations
                WHERE workspace_id=? AND operation_id=?::uuid FOR UPDATE
                """, workspaceId, request.operationId().toString());
        if (records.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reset operation not found");
        var stored = records.get(0);
        if (((Number)stored.get("requested_by_member_id")).longValue() != memberId)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only initiator can confirm");
        if (!request.snapshotDigest().equals(stored.get("snapshot_digest")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reset preview changed");
        String status = (String)stored.get("status");
        if ("CONFIRMED".equals(status))
            return new ConfirmedReset(request.operationId(), "CONFIRMED", false);
        if (!"PREPARED".equals(status))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reset operation cannot be confirmed");
        db.update("""
                UPDATE workspace_reset_operations SET status='CONFIRMED', confirmed_at=now()
                WHERE workspace_id=? AND operation_id=?::uuid AND status='PREPARED'
                """, workspaceId, request.operationId().toString());
        return new ConfirmedReset(request.operationId(), "CONFIRMED", false);
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
