package com.olehkobylianskyi.familypoints.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * C -> B: immutable, action-specific scope descriptor for future durable
 * PostgreSQL owner approvals. A validated descriptor is NOT a grant, credential,
 * signed token or authorization to execute a Reset/Restore operation.
 *
 * Concrete durable storage, verified owner resolver, transport and atomic consume
 * remain gated on the architecture IMP and Backend B consumer review.
 */
public final class WorkspaceDestructivePermitContract {
    private WorkspaceDestructivePermitContract() {}

    public enum Action { RESET, RESTORE }

    private static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");
    public static final Duration MAX_LIFETIME = Duration.ofMinutes(5);

    public record Scope(
            Action action,
            UUID operationId,
            long workspaceId,
            long ownerAccountId,
            long ownerMemberId,
            UUID snapshotId,
            String snapshotDigest,
            String selectionDigest,
            String memberMappingDigest,
            long dataEpoch,
            Instant issuedAt,
            Instant expiresAt
    ) {
        public Scope {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(operationId, "operationId");
            Objects.requireNonNull(snapshotId, "snapshotId");
            Objects.requireNonNull(issuedAt, "issuedAt");
            Objects.requireNonNull(expiresAt, "expiresAt");
            if (workspaceId <= 0 || ownerAccountId <= 0 || ownerMemberId <= 0
                    || dataEpoch < 0 || dataEpoch == Long.MAX_VALUE)
                throw new IllegalArgumentException("Invalid target or owner identity/epoch");
            requireDigest(snapshotDigest, "snapshotDigest");
            requireDigest(selectionDigest, "selectionDigest");
            if (action == Action.RESTORE) {
                requireDigest(memberMappingDigest, "memberMappingDigest");
            } else if (memberMappingDigest != null) {
                throw new IllegalArgumentException("RESET cannot carry RESTORE member mapping");
            }
            Duration lifetime = Duration.between(issuedAt, expiresAt);
            if (lifetime.isNegative() || lifetime.isZero()
                    || lifetime.compareTo(MAX_LIFETIME) > 0)
                throw new IllegalArgumentException("Permit validity must be within five minutes");
        }

        /** Validates attempted operation against the immutable server-verified grant scope. */
        public boolean matches(Scope attempted, Instant now) {
            return attempted != null && now != null
                    && !now.isBefore(issuedAt) && now.isBefore(expiresAt)
                    && action == attempted.action
                    && operationId.equals(attempted.operationId)
                    && workspaceId == attempted.workspaceId
                    && ownerAccountId == attempted.ownerAccountId
                    && ownerMemberId == attempted.ownerMemberId
                    && snapshotId.equals(attempted.snapshotId)
                    && snapshotDigest.equals(attempted.snapshotDigest)
                    && selectionDigest.equals(attempted.selectionDigest)
                    && Objects.equals(memberMappingDigest, attempted.memberMappingDigest)
                    && dataEpoch == attempted.dataEpoch
                    && issuedAt.equals(attempted.issuedAt)
                    && expiresAt.equals(attempted.expiresAt);
        }

        /** Excludes scope hashes and identity from logs by default. */
        @Override public String toString() {
            return "WorkspaceDestructivePermitScope[action=" + action + ", redacted]";
        }
    }

    private static void requireDigest(String value, String field) {
        if (value == null || !DIGEST.matcher(value).matches())
            throw new IllegalArgumentException(field + " must be lowercase SHA-256 hex");
    }
}
