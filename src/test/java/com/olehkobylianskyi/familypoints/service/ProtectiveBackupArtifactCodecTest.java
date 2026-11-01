package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProtectiveBackupArtifactCodecTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private static SecretKey key() throws Exception {
        var generator=KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }
    @Test void authenticatedRoundTripAndDistinctRandomNonces() throws Exception {
        var id=UUID.randomUUID();var op=UUID.randomUUID();var key=key();
        byte[] source="{\"workspace\":42,\"items\":[1,2]}".getBytes(StandardCharsets.UTF_8);
        var first=codec.seal(id,42L,op,3L,"a".repeat(64),"V50",source,key);
        var second=codec.seal(id,42L,op,3L,"a".repeat(64),"V50",source,key);
        assertArrayEquals(source,codec.open(first,id,42L,key));
        assertEquals(ProtectiveBackupArtifactCodec.sha(source),first.manifest().contentSha256());
        assertFalse(java.util.Arrays.equals(first.ciphertext(),second.ciphertext()));
        assertEquals(source.length,first.manifest().contentBytes());
    }
    @Test void corruptCiphertextAndManifestAndForeignTenantFailClosed() throws Exception {
        var id=UUID.randomUUID();var op=UUID.randomUUID();var key=key();
        var original=codec.seal(id,17L,op,0L,"b".repeat(64),"V50",
                "sensitive family data".getBytes(StandardCharsets.UTF_8),key);
        assertThrows(SecurityException.class,()->codec.open(original,id,18L,key));
        assertThrows(SecurityException.class,()->codec.open(original,UUID.randomUUID(),17L,key));
        assertThrows(SecurityException.class,()->codec.open(original,id,17L,key()));
        byte[] damaged=original.ciphertext().clone();damaged[damaged.length-1]^=1;
        assertThrows(SecurityException.class,()->codec.open(
                new ProtectiveBackupArtifactCodec.EncryptedArtifact(original.manifest(),damaged),
                id,17L,key));
        var m=original.manifest();
        var tampered=new ProtectiveBackupArtifactCodec.Manifest(m.snapshotId(),m.workspaceId(),
                m.operationId(),m.dataEpoch()+1,m.sourceDigest(),m.schemaVersion(),m.createdAt(),
                m.contentSha256(),m.contentBytes());
        assertThrows(SecurityException.class,()->codec.open(
                new ProtectiveBackupArtifactCodec.EncryptedArtifact(tampered,original.ciphertext()),
                id,17L,key));
    }
    @Test void rejectsUnscopedAndInvalidBackupMetadata() throws Exception {
        var key=key();var id=UUID.randomUUID();var op=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->codec.seal(id,0L,op,0L,
                "a".repeat(64),"V50",new byte[0],key));
        assertThrows(IllegalArgumentException.class,()->codec.seal(id,1L,op,-1L,
                "a".repeat(64),"V50",new byte[0],key));
        assertThrows(IllegalArgumentException.class,()->codec.seal(id,1L,op,0L,
                "wrong","V50",new byte[0],key));
    }
}
