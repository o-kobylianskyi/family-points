package com.olehkobylianskyi.familypoints.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Review-only shape for a future audited original-owner registration / legacy
 * enrollment process. This class DOES NOT authenticate evidence, bind an owner,
 * write a database row or provide authorization. A trusted independent verifier
 * and a transactional registration writer are required.
 *
 * No admin role, earliest family member, email, archived member, or name may
 * stand in for independently verified provenance.
 */
public final class OriginalOwnerEnrollmentEvidence {
    private OriginalOwnerEnrollmentEvidence() {}

    public enum Method {
        AUTHENTICATED_REGISTRATION,
        LEGACY_INDEPENDENT_VERIFICATION
    }
    public enum EvidenceKind {
        REGISTERED_CREATOR_EVENT,
        VERIFIED_ACCOUNT_RECOVERY_CASE
    }

    public record Candidate(
            long workspaceId,
            long originalAccountId,
            long originalMemberId,
            Method method,
            EvidenceKind evidenceKind,
            UUID evidenceId,
            String reviewerPrincipal,
            Instant verifiedAt
    ) {
        public Candidate {
            if (workspaceId <= 0 || originalAccountId <= 0 || originalMemberId <= 0
                    || method == null || evidenceKind == null || evidenceId == null
                    || reviewerPrincipal == null || reviewerPrincipal.isBlank()
                    || verifiedAt == null)
                throw new IllegalArgumentException("Incomplete owner provenance");
            if (reviewerPrincipal.length() > 256)
                throw new IllegalArgumentException("Invalid reviewer principal");
            if (method == Method.AUTHENTICATED_REGISTRATION
                    && evidenceKind != EvidenceKind.REGISTERED_CREATOR_EVENT)
                throw new IllegalArgumentException("Registration must use verified creator event");
            if (method == Method.LEGACY_INDEPENDENT_VERIFICATION
                    && evidenceKind != EvidenceKind.VERIFIED_ACCOUNT_RECOVERY_CASE)
                throw new IllegalArgumentException("Legacy owner requires independent recovery evidence");
        }
        @Override public String toString() { return "OwnerEnrollmentCandidate[redacted]"; }
    }

    public record CurrentMembership(
            long workspaceId,
            long accountId,
            long memberId,
            boolean accountEnabled,
            boolean memberActive,
            boolean memberDeleted,
            boolean exactAccountMemberLinkPresent
    ) {}

    /**
     * Pure final preflight. trustedEvidenceVerified must be returned by an
     * independent server-side verifier, NEVER copied from client input.
     * Enrollment must still occur in one DB transaction with the family lock,
     * auditable unique original-owner row and registration/recovery record.
     */
    public static boolean permitsEnrollment(
            Candidate candidate,
            CurrentMembership current,
            boolean trustedEvidenceVerified,
            boolean existingOriginalOwnerBinding,
            Instant currentServerTime
    ) {
        if (candidate == null || current == null || currentServerTime == null
                || !trustedEvidenceVerified || existingOriginalOwnerBinding)
            return false;
        if (candidate.workspaceId() != current.workspaceId()
                || candidate.originalAccountId() != current.accountId()
                || candidate.originalMemberId() != current.memberId()
                || !current.accountEnabled() || !current.memberActive()
                || current.memberDeleted() || !current.exactAccountMemberLinkPresent())
            return false;
        // Registration should happen together with family creation; a delayed
        // registration decision fails closed rather than turning into legacy.
        Duration age = Duration.between(candidate.verifiedAt(), currentServerTime);
        Duration maximum = candidate.method() == Method.AUTHENTICATED_REGISTRATION
                ? Duration.ofMinutes(5) : Duration.ofMinutes(15);
        return !age.isNegative() && age.compareTo(maximum) <= 0;
    }
}
