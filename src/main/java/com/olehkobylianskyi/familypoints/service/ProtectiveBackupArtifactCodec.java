package com.olehkobylianskyi.familypoints.service;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * BKL-BACKUP-001 portable authenticated encryption format, not a backup exporter
 * or restore engine. Caller must supply an external 256-bit KMS-managed key;
 * this class never stores keys, reads tenant tables or promotes VERIFIED.
 */
@Component
public final class ProtectiveBackupArtifactCodec {
    private static final byte[] MAGIC="FPBACKUP1".getBytes(StandardCharsets.US_ASCII);
    private static final int NONCE_LENGTH=12;
    private static final int MAX_PLAINTEXT=64*1024*1024;
    private final SecureRandom random=new SecureRandom();

    public record Manifest(UUID snapshotId,long workspaceId,UUID operationId,long dataEpoch,
                           String sourceDigest,String schemaVersion,Instant createdAt,
                           String contentSha256,long contentBytes) {}
    public record EncryptedArtifact(Manifest manifest,byte[] ciphertext) {}

    public EncryptedArtifact seal(UUID snapshotId,long workspaceId,UUID operationId,long dataEpoch,
            String sourceDigest,String schemaVersion,byte[] plaintext,SecretKey key) {
        if(snapshotId==null || operationId==null || workspaceId<=0 || dataEpoch<0
            || sourceDigest==null || !sourceDigest.matches("[0-9a-f]{64}")
            || schemaVersion==null || schemaVersion.isBlank() || schemaVersion.length()>80
            || plaintext==null || plaintext.length>MAX_PLAINTEXT)
            throw new IllegalArgumentException("Invalid backup artifact metadata");
        checkKey(key);
        try {
            var manifest=new Manifest(snapshotId,workspaceId,operationId,dataEpoch,
                sourceDigest,schemaVersion,Instant.now(),sha(plaintext),plaintext.length);
            byte[] associated=aad(manifest);
            byte[] nonce=new byte[NONCE_LENGTH];random.nextBytes(nonce);
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
            cipher.updateAAD(associated);
            byte[] encrypted=cipher.doFinal(plaintext);
            return new EncryptedArtifact(manifest,ByteBuffer.allocate(MAGIC.length+NONCE_LENGTH+encrypted.length)
                .put(MAGIC).put(nonce).put(encrypted).array());
        }catch(Exception e){throw new IllegalStateException("Backup encryption failed",e);}
    }

    public byte[] open(EncryptedArtifact artifact,UUID snapshotId,long workspaceId,SecretKey key) {
        Objects.requireNonNull(artifact,"artifact");
        checkKey(key);
        var manifest=Objects.requireNonNull(artifact.manifest(),"manifest");
        if(!manifest.snapshotId().equals(snapshotId) || manifest.workspaceId()!=workspaceId)
            throw new SecurityException("Backup scope mismatch");
        byte[] data=artifact.ciphertext();
        if(data==null || data.length<MAGIC.length+NONCE_LENGTH+16 ||
                !java.util.Arrays.equals(java.util.Arrays.copyOf(data,MAGIC.length),MAGIC))
            throw new SecurityException("Invalid backup envelope");
        try {
            var nonce=java.util.Arrays.copyOfRange(data,MAGIC.length,MAGIC.length+NONCE_LENGTH);
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,nonce));
            cipher.updateAAD(aad(manifest));
            byte[] content=cipher.doFinal(data,MAGIC.length+NONCE_LENGTH,
                data.length-MAGIC.length-NONCE_LENGTH);
            if(content.length!=manifest.contentBytes() ||
                    !MessageDigest.isEqual(sha(content).getBytes(StandardCharsets.US_ASCII),
                        manifest.contentSha256().getBytes(StandardCharsets.US_ASCII)))
                throw new SecurityException("Backup content digest mismatch");
            return content;
        }catch(SecurityException e){throw e;}
        catch(Exception e){throw new SecurityException("Backup authentication failed",e);}
    }

    private static byte[] aad(Manifest m) {
        // Deterministic length-prefixed metadata (no JavaTime Jackson module dependency).
        var fields=new String[]{m.snapshotId().toString(),Long.toString(m.workspaceId()),
                m.operationId().toString(),Long.toString(m.dataEpoch()),m.sourceDigest(),
                m.schemaVersion(),m.createdAt().toString(),m.contentSha256(),
                Long.toString(m.contentBytes())};
        var joined=new StringBuilder();
        for(String field:fields)joined.append(field.length()).append(':').append(field);
        return joined.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void checkKey(SecretKey key) {
        if(key==null || !"AES".equalsIgnoreCase(key.getAlgorithm()) ||
                key.getEncoded()==null || key.getEncoded().length!=32)
            throw new IllegalArgumentException("Externally supplied AES-256 key required");
    }
    public static String sha(byte[] bytes) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
}
