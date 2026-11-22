package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.security.OriginalOwnerEnrollmentEvidence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side provenance writer. Only a genuinely new family registration/
 * creation transaction may enroll its creator. Existing legacy families are
 * NOT inferred from admin roles, earliest member or login email.
 */
@Service
public class WorkspaceOriginalOwnerEnrollmentService {
    private final JdbcTemplate db;
    public WorkspaceOriginalOwnerEnrollmentService(JdbcTemplate db) { this.db=db; }

    @Transactional(propagation=Propagation.MANDATORY)
    public void enrollNewCreator(long workspaceId,long accountId,long memberId) {
        if(workspaceId<=0||accountId<=0||memberId<=0)
            throw new SecurityException("RESTORE_OWNER_ENROLLMENT_SCOPE_INVALID");
        // Never infer creation from age, administrator role or membership.
        // The exact family/member must have been provisioned in this SAME
        // physical transaction; any later legacy enrollment fails closed.
        WorkspaceNewFamilyTransactionEvidence.consume(workspaceId,memberId);
        Long count=db.queryForObject("""
                SELECT count(*) FROM families f
                JOIN family_members m ON m.family_id=f.id AND m.id=?
                JOIN account_workspace_memberships l
                  ON l.workspace_id=f.id AND l.family_member_id=m.id AND l.account_id=?
                JOIN user_accounts a ON a.id=l.account_id
                WHERE f.id=? AND a.enabled=true AND m.active=true
                  AND m.deleted_at IS NULL
                """,Long.class,memberId,accountId,workspaceId);
        if(count==null||count!=1)
            throw new SecurityException("RESTORE_CREATOR_IDENTITY_NOT_LINKED");
        // The evidence is created by this trusted application path, not from
        // a submitted JWT role, an arbitrary Candidate or client identifier.
        var now=Instant.now();
        var candidate=new OriginalOwnerEnrollmentEvidence.Candidate(
                workspaceId,accountId,memberId,
                OriginalOwnerEnrollmentEvidence.Method.AUTHENTICATED_REGISTRATION,
                OriginalOwnerEnrollmentEvidence.EvidenceKind.REGISTERED_CREATOR_EVENT,
                UUID.randomUUID(),"internal:family-creation",now);
        var current=new OriginalOwnerEnrollmentEvidence.CurrentMembership(
                workspaceId,accountId,memberId,true,true,false,true);
        if(!OriginalOwnerEnrollmentEvidence.permitsEnrollment(
                candidate,current,true,false,now))
            throw new SecurityException("RESTORE_CREATOR_EVIDENCE_REJECTED");
        int written=db.update("""
                INSERT INTO workspace_original_owner_bindings
                    (workspace_id,owner_account_id,owner_member_id,provenance,evidence_id)
                VALUES (?,?,?,'REGISTRATION_VERIFIED',?)
                ON CONFLICT (workspace_id) DO NOTHING
                """,workspaceId,accountId,memberId,candidate.evidenceId());
        if(written!=1)
            throw new SecurityException("RESTORE_CREATOR_ALREADY_ENROLLED");
    }
}
