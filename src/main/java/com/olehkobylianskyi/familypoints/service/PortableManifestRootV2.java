package com.olehkobylianskyi.familypoints.service;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * IMP-01 canonical binary manifest-root encoder. A digest is not itself a
 * signature, an owner grant, or evidence that the supplied category bytes were
 * authenticated. Exporter/inspector must verify per-category bytes separately.
 */
public final class PortableManifestRootV2 {
    public static final String SCHEMA="family-points-export-v2";
    public static final String DOMAIN="family-points:portable:manifest-root:v2";
    public record Category(long rows,String contentSha256,String dependencyClosureSha256) {}
    private PortableManifestRootV2() {}

    public static String digest(long sourceWorkspaceId,UUID snapshotId,long sourceEpoch,
                                Map<String,Category> categories) {
        if(sourceWorkspaceId<=0||snapshotId==null||sourceEpoch<0
                ||categories==null||categories.isEmpty()||categories.size()>64)
            throw new SecurityException("PORTABLE_MANIFEST_SCOPE_INVALID");
        var ordered=new ArrayList<>(categories.keySet());
        if(ordered.stream().anyMatch(Objects::isNull)
                ||ordered.stream().anyMatch(s->!s.matches("[A-Z][A-Z0-9_]{0,63}")))
            throw new SecurityException("PORTABLE_MANIFEST_CATEGORY_INVALID");
        // The protocol sorts by unsigned UTF-8 bytes, not JSON object order,
        // locale or a frontend-specific comparator. Allowed codes are ASCII.
        ordered.sort((a,b)->Arrays.compareUnsigned(
                a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8)));
        try {
            var memory=new ByteArrayOutputStream();
            var out=new DataOutputStream(memory);
            write(out,DOMAIN);
            out.writeLong(sourceWorkspaceId);
            write(out,SCHEMA);
            out.writeLong(snapshotId.getMostSignificantBits());
            out.writeLong(snapshotId.getLeastSignificantBits());
            out.writeLong(sourceEpoch);
            out.writeInt(ordered.size());
            for(String code:ordered) {
                Category category=categories.get(code);
                if(category==null||category.rows()<0||category.rows()>2000)
                    throw new SecurityException("PORTABLE_MANIFEST_ROWS_INVALID");
                write(out,code);
                out.writeLong(category.rows());
                out.write(shaBytes(category.contentSha256()));
                out.write(shaBytes(category.dependencyClosureSha256()));
            }
            out.flush();
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(memory.toByteArray()));
        }catch(IOException|NoSuchAlgorithmException impossible){
            throw new IllegalStateException("Portable manifest SHA-256 unavailable",impossible);
        }
    }
    private static void write(DataOutputStream out,String value) throws IOException {
        byte[] bytes=value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
    private static byte[] shaBytes(String digest) {
        if(digest==null||!digest.matches("[0-9a-f]{64}"))
            throw new SecurityException("PORTABLE_MANIFEST_DIGEST_INVALID");
        return HexFormat.of().parseHex(digest);
    }
}
