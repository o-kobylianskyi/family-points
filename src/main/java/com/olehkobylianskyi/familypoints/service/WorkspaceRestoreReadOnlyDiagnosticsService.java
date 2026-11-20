package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * B-NEXT-02. INTERNAL read-only operational diagnostics for a specific RESTORE
 * operation. No controller, token, artifact, source data, or retry capability.
 *
 * An authenticated selected workspace member is required even though this is
 * not a public endpoint. The result is deliberately PII/credential-free.
 * The V53 receipt itself is not modified, rehashed or interpreted as consent.
 */
@Service
public class WorkspaceRestoreReadOnlyDiagnosticsService {
    public enum State {
        RESTORE_SCHEMA_NOT_DEPLOYED,
        OPERATION_NOT_FOUND,
        PERMIT_OUTSTANDING,
        PERMIT_EXPIRED_UNUSED,
        PERMIT_CONSUMED_WITHOUT_RECEIPT,
        RECEIPT_COMMITTED,
        RECEIPT_INCONSISTENT
    }

    public record Diagnostic(UUID operationId,long workspaceId,State state,
                             Long epochBefore,Long epochAfter,
                             boolean executable,List<String> advisoryCodes) {
        public Diagnostic {
            advisoryCodes=List.copyOf(advisoryCodes);
            if(executable)
                throw new SecurityException("RESTORE_DIAGNOSTIC_MUST_NOT_AUTHORIZE_EXECUTION");
        }
    }

    private final JdbcTemplate db;
    private final CurrentUserService current;
    public WorkspaceRestoreReadOnlyDiagnosticsService(JdbcTemplate db,CurrentUserService current) {
        this.db=Objects.requireNonNull(db);
        this.current=Objects.requireNonNull(current);
    }

    @Transactional(readOnly=true)
    public Diagnostic assess(long workspaceId,UUID operationId) {
        if(workspaceId<=0||operationId==null)
            throw new SecurityException("RESTORE_DIAGNOSTIC_SCOPE_INVALID");
        var account=current.getCurrentAccount();
        var member=current.getCurrentMember();
        if(account==null||!account.isEnabled()||account.getId()==null
                ||member==null||member.getId()==null
                ||member.getWorkspace()==null||member.getWorkspace().getId()==null
                ||member.getWorkspace().getId().longValue()!=workspaceId)
            throw new SecurityException("RESTORE_DIAGNOSTIC_SELECTED_WORKSPACE_REQUIRED");

        Boolean ready=db.queryForObject("""
                SELECT to_regclass('public.workspace_restore_receipts') IS NOT NULL
                   AND to_regclass('public.workspace_restore_permits') IS NOT NULL
                """,Boolean.class);
        if(!Boolean.TRUE.equals(ready))
            return view(operationId,workspaceId,State.RESTORE_SCHEMA_NOT_DEPLOYED,
                    null,null,"RESTORE_SCHEMA_NOT_DEPLOYED");

        // The two queries are tenant-fenced. No global operation lookup that
        // could disclose whether another family used a particular UUID.
        var receipts=db.queryForList("""
                SELECT target_epoch_before,target_epoch_after
                FROM workspace_restore_receipts
                WHERE target_workspace_id=? AND operation_id=?
                """,workspaceId,operationId);
        if(receipts.size()>1)
            throw new SecurityException("RESTORE_RECEIPT_NON_UNIQUE");
        if(!receipts.isEmpty()) {
            Map<String,Object> row=receipts.get(0);
            Long before=number(row.get("target_epoch_before"));
            Long after=number(row.get("target_epoch_after"));
            if(before==null||after==null||before<0||after!=before+1||before==Long.MAX_VALUE)
                return view(operationId,workspaceId,State.RECEIPT_INCONSISTENT,
                        null,null,"RESTORE_RECEIPT_REVIEW_REQUIRED");
            return view(operationId,workspaceId,State.RECEIPT_COMMITTED,
                    before,after,"RESTORE_RECEIPT_READ_ONLY");
        }

        var permits=db.queryForList("""
                SELECT consumed_at,expires_at
                FROM workspace_restore_permits
                WHERE target_workspace_id=? AND operation_id=?
                """,workspaceId,operationId);
        if(permits.size()>1)
            throw new SecurityException("RESTORE_PERMIT_NON_UNIQUE");
        if(permits.isEmpty())
            return view(operationId,workspaceId,State.OPERATION_NOT_FOUND,
                    null,null,"RESTORE_OPERATION_NOT_FOUND");
        var permit=permits.get(0);
        if(permit.get("consumed_at")!=null)
            return view(operationId,workspaceId,State.PERMIT_CONSUMED_WITHOUT_RECEIPT,
                    null,null,"RESTORE_CONSUMED_WITHOUT_RECEIPT_REVIEW_REQUIRED");
        var expiration=permit.get("expires_at");
        if(!(expiration instanceof java.util.Date date))
            return view(operationId,workspaceId,State.RECEIPT_INCONSISTENT,
                    null,null,"RESTORE_PERMIT_TIMESTAMP_INVALID");
        if(!date.toInstant().isAfter(Instant.now()))
            return view(operationId,workspaceId,State.PERMIT_EXPIRED_UNUSED,
                    null,null,"RESTORE_PERMIT_EXPIRED_NO_AUTOMATIC_RETRY");
        return view(operationId,workspaceId,State.PERMIT_OUTSTANDING,
                null,null,"RESTORE_PENDING_IS_NOT_PROOF_OF_SUCCESS");
    }

    private static Long number(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
    private static Diagnostic view(UUID operationId,long workspaceId,State state,
                                   Long before,Long after,String code) {
        return new Diagnostic(operationId,workspaceId,state,before,after,false,List.of(code));
    }
}
