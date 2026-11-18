package com.olehkobylianskyi.familypoints.security;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * C -> B reviewable RESTORE permit scope v2, not an issued credential/grant.
 *
 * Protects signed source identity (source tenant + content schema + manifest),
 * target owner identity, snapshot/source epoch and distinct target epoch.
 * This pure contract has no persistent storage and no HTTP integration.
 */
public final class WorkspaceRestorePermitScopeV2 {
    public static final String MAPPING_DOMAIN = "family-points:restore:member-map:v2";
    public static final String SCOPE_DOMAIN = "family-points:restore:scope:v2";
    public static final Duration MAX_TTL = Duration.ofMinutes(5);
    private WorkspaceRestorePermitScopeV2() {}

    /** An archive identity is not proof of ownership of the target. */
    public record SourceArchive(
            long sourceWorkspaceId,
            String schemaVersion,
            UUID snapshotId,
            long sourceSnapshotEpoch,
            String snapshotDigest,
            String manifestRootDigest
    ) {
        public SourceArchive {
            if (sourceWorkspaceId <= 0 || sourceSnapshotEpoch < 0
                    || snapshotId == null || schemaVersion == null
                    || !schemaVersion.matches("[A-Za-z0-9._-]{1,40}"))
                throw new IllegalArgumentException("Invalid source archive identity");
            requireDigest(snapshotDigest);
            requireDigest(manifestRootDigest);
        }
    }

    /** Values must originate from owner-reviewed, validated source/target mapping. */
    public record RestoreScope(
            long targetWorkspaceId,
            long authorizedOwnerAccountId,
            long authorizedOwnerMemberId,
            UUID operationId,
            SourceArchive source,
            String selectionClosureDigest,
            String memberMappingDigest,
            long targetExpectedEpoch,
            Instant issuedAt,
            Instant expiresAt
    ) {
        public RestoreScope {
            if (targetWorkspaceId <= 0 || authorizedOwnerAccountId <= 0
                    || authorizedOwnerMemberId <= 0 || operationId == null
                    || source == null || targetExpectedEpoch < 0
                    || targetExpectedEpoch == Long.MAX_VALUE
                    || issuedAt == null || expiresAt == null)
                throw new IllegalArgumentException("Invalid target/owner identity");
            requireDigest(selectionClosureDigest);
            requireDigest(memberMappingDigest);
            Duration ttl=Duration.between(issuedAt,expiresAt);
            if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(MAX_TTL)>0)
                throw new IllegalArgumentException("Invalid permit lifetime");
        }
        public boolean matches(RestoreScope attempted, Instant currentServerTime) {
            return attempted != null && currentServerTime != null
                    && !currentServerTime.isBefore(issuedAt)
                    && currentServerTime.isBefore(expiresAt)
                    && equals(attempted);
        }
        @Override public String toString() {
            return "RestoreScopeV2[redacted]";
        }
    }

    /**
     * Canonical source->target member hash. Binary format: length-prefixed
     * UTF-8 domain; 64-bit source/target family IDs; 32-bit cardinality;
     * each sorted pair as two signed 64-bit integers (positive only).
     * No JSON ordering, names, role codes or UI identifiers participate.
     *
     * Rejects duplicate target IDs (not injective), null/invalid IDs, empty map
     * and excessive collection size. A consumer must also verify all source IDs
     * belong to the signed source roster and target IDs to target membership.
     */
    public static String canonicalMappingDigest(long sourceWorkspaceId,
                                                 long targetWorkspaceId,
                                                 Map<Long,Long> reviewedPairs) {
        if (sourceWorkspaceId <= 0 || targetWorkspaceId <= 0
                || reviewedPairs == null || reviewedPairs.isEmpty()
                || reviewedPairs.size() > 100_000)
            throw new IllegalArgumentException("Invalid mapping scope");
        var sorted = new TreeMap<Long,Long>();
        var targets = new HashSet<Long>();
        for (var entry : reviewedPairs.entrySet()) {
            Long source=entry.getKey(), target=entry.getValue();
            if (source == null || target == null || source <= 0 || target <= 0
                    || !targets.add(target))
                throw new IllegalArgumentException("Member mapping is invalid/non-injective");
            sorted.put(source,target);
        }
        try {
            var bytes = new ByteArrayOutputStream();
            var output = new DataOutputStream(bytes);
            writeDomain(output,MAPPING_DOMAIN);
            output.writeLong(sourceWorkspaceId);
            output.writeLong(targetWorkspaceId);
            output.writeInt(sorted.size());
            for (var entry : sorted.entrySet()) {
                output.writeLong(entry.getKey());
                output.writeLong(entry.getValue());
            }
            output.flush();
            return sha(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Domain-separated binary scope identity for stored permit comparison. */
    public static String canonicalScopeDigest(RestoreScope scope) {
        Objects.requireNonNull(scope,"scope");
        try {
            var bytes=new ByteArrayOutputStream();
            var output=new DataOutputStream(bytes);
            writeDomain(output,SCOPE_DOMAIN);
            output.writeLong(scope.targetWorkspaceId());
            output.writeLong(scope.authorizedOwnerAccountId());
            output.writeLong(scope.authorizedOwnerMemberId());
            writeUuid(output,scope.operationId());
            var source=scope.source();
            output.writeLong(source.sourceWorkspaceId());
            writeDomain(output,source.schemaVersion());
            writeUuid(output,source.snapshotId());
            output.writeLong(source.sourceSnapshotEpoch());
            writeDigest(output,source.snapshotDigest());
            writeDigest(output,source.manifestRootDigest());
            writeDigest(output,scope.selectionClosureDigest());
            writeDigest(output,scope.memberMappingDigest());
            output.writeLong(scope.targetExpectedEpoch());
            output.writeLong(scope.issuedAt().getEpochSecond());
            output.writeInt(scope.issuedAt().getNano());
            output.writeLong(scope.expiresAt().getEpochSecond());
            output.writeInt(scope.expiresAt().getNano());
            output.flush();
            return sha(bytes.toByteArray());
        } catch(IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void writeDomain(DataOutputStream output,String domain) throws IOException {
        byte[] utf8=domain.getBytes(StandardCharsets.UTF_8);
        output.writeInt(utf8.length);
        output.write(utf8);
    }
    private static void writeUuid(DataOutputStream output,UUID id) throws IOException {
        output.writeLong(id.getMostSignificantBits());
        output.writeLong(id.getLeastSignificantBits());
    }
    private static void writeDigest(DataOutputStream output,String hex) throws IOException {
        output.write(HexFormat.of().parseHex(hex));
    }
    private static void requireDigest(String value) {
        if (value==null || !value.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Expected lowercase SHA-256 digest");
    }
    private static String sha(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
