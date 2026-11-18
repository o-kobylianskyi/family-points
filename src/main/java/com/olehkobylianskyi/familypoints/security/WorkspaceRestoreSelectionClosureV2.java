package com.olehkobylianskyi.familypoints.security;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * C/B reviewable digest of immutable owner-reviewed RESTORE category selection.
 * Produces only a canonical fingerprint; does not decide if a dependency graph
 * is complete or if category data is safe to restore.
 */
public final class WorkspaceRestoreSelectionClosureV2 {
    public static final String DOMAIN = "family-points:restore:selection-closure:v2";

    private WorkspaceRestoreSelectionClosureV2() {}

    public enum Decision { RESTORE, PRESERVE, BLOCK }

    public record Category(
            String categoryCode,
            Decision decision,
            long sourceRowCount,
            String sourceCategoryDigest,
            String dependencyClosureDigest,
            String targetConflictPlanDigest
    ) {
        public Category {
            if (categoryCode == null || !categoryCode.matches("[A-Z][A-Z0-9_]{1,63}")
                    || decision == null || sourceRowCount < 0) {
                throw new IllegalArgumentException("Invalid selection category");
            }
            digest(sourceCategoryDigest);
            digest(dependencyClosureDigest);
            digest(targetConflictPlanDigest);
        }
    }

    /**
     * Hashes all explicitly considered categories, including PRESERVE and BLOCK.
     * The consumer must independently verify the list is the complete catalog
     * for the signed manifest and validate every dependency/financial closure.
     */
    public static String canonicalDigest(String schemaVersion,
                                         long sourceWorkspaceId,
                                         long targetWorkspaceId,
                                         Collection<Category> categories) {
        if (schemaVersion == null || !schemaVersion.matches("[A-Za-z0-9._-]{1,40}")
                || sourceWorkspaceId <= 0 || targetWorkspaceId <= 0
                || categories == null || categories.isEmpty() || categories.size() > 10_000)
            throw new IllegalArgumentException("Invalid selection scope");
        Set<String> names=new HashSet<>();
        for (Category category: categories) {
            if (category == null || !names.add(category.categoryCode()))
                throw new IllegalArgumentException("Duplicate/null selection category");
        }
        List<Category> ordered=categories.stream()
                .sorted(Comparator.comparing(Category::categoryCode)).toList();
        try {
            var bytes=new ByteArrayOutputStream();
            var out=new DataOutputStream(bytes);
            write(out,DOMAIN);
            write(out,schemaVersion);
            out.writeLong(sourceWorkspaceId);
            out.writeLong(targetWorkspaceId);
            out.writeInt(ordered.size());
            for (Category c:ordered) {
                write(out,c.categoryCode());
                write(out,c.decision().name());
                out.writeLong(c.sourceRowCount());
                writeHex(out,c.sourceCategoryDigest());
                writeHex(out,c.dependencyClosureDigest());
                writeHex(out,c.targetConflictPlanDigest());
            }
            out.flush();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(IOException | NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void write(DataOutputStream out,String value) throws IOException {
        byte[] chars=value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(chars.length);
        out.write(chars);
    }
    private static void writeHex(DataOutputStream out,String value) throws IOException {
        out.write(HexFormat.of().parseHex(Objects.requireNonNull(value)));
    }
    private static void digest(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid SHA-256 category fingerprint");
    }
}
