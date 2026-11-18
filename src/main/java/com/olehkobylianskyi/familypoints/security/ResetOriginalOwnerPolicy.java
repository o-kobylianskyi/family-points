package com.olehkobylianskyi.familypoints.security;

import java.util.Objects;

/**
 * Fail-closed original-owner decision for the BKL-002 destructive Reset boundary.
 *
 * This policy deliberately has no database access or role-name heuristics.
 * Its caller MUST obtain an authoritative, verified owner binding and current
 * account/member/membership state inside the appropriate workspace transaction.
 * This class alone never authorizes Reset execution or a Restore operation.
 *
 * Integration with the public Reset controllers awaits approval of canonical
 * owner provenance, registration writes and Flyway migration.
 */
public final class ResetOriginalOwnerPolicy {

    private ResetOriginalOwnerPolicy() {}

    public enum Provenance {
        REGISTRATION_VERIFIED,
        LEGACY_VERIFIED,
        UNKNOWN
    }

    /** Trusted persisted binding; never construct from a role or member ordering. */
    public record VerifiedOwnerBinding(
            Long workspaceId,
            Long ownerAccountId,
            Long ownerMemberId,
            Provenance provenance
    ) {}

    /** Freshly resolved authenticated principal and its target-workspace linkage. */
    public record CurrentActor(
            Long workspaceId,
            Long accountId,
            Long memberId,
            boolean authenticated,
            boolean accountEnabled,
            boolean membershipActive,
            boolean memberDeleted,
            boolean accountMemberLinkPresent
    ) {}

    /**
     * Eligibility is a necessary but NOT sufficient precondition for Reset:
     * still require fresh credential, bound approval, scope and epoch verification.
     */
    public static boolean isVerifiedOriginalOwner(
            Long requestedWorkspaceId,
            CurrentActor actor,
            VerifiedOwnerBinding binding
    ) {
        if (requestedWorkspaceId == null || actor == null || binding == null) {
            return false;
        }
        if (binding.provenance() != Provenance.REGISTRATION_VERIFIED
                && binding.provenance() != Provenance.LEGACY_VERIFIED) {
            return false;
        }
        if (!actor.authenticated() || !actor.accountEnabled()
                || !actor.membershipActive() || actor.memberDeleted()
                || !actor.accountMemberLinkPresent()) {
            return false;
        }
        if (actor.accountId() == null || actor.memberId() == null
                || binding.ownerAccountId() == null || binding.ownerMemberId() == null) {
            return false;
        }
        return Objects.equals(requestedWorkspaceId, actor.workspaceId())
                && Objects.equals(requestedWorkspaceId, binding.workspaceId())
                && Objects.equals(actor.accountId(), binding.ownerAccountId())
                && Objects.equals(actor.memberId(), binding.ownerMemberId());
    }
}
