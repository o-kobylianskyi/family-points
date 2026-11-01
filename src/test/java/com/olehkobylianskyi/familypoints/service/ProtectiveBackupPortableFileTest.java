package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProtectiveBackupPortableFileTest {
    @Test void encryptedArtifactSurvivesFileTransferAndAuthenticatesIdentity() throws Exception {
        var keys=KeyGenerator.getInstance("AES");keys.init(256);var key=keys.generateKey();
        var codec=new ProtectiveBackupArtifactCodec();
        var format=new ProtectiveBackupPortableFile();
        var snapshot=UUID.randomUUID();var operation=UUID.randomUUID();
        byte[] source="tenant scoped private fixture".getBytes(StandardCharsets.UTF_8);
        var sealed=codec.seal(snapshot,42L,operation,0L,"a".repeat(64),"V50",source,key);
        byte[] bytes=format.encode(sealed);
        assertArrayEquals(source,codec.open(format.decode(bytes),snapshot,42L,key));
        assertThrows(SecurityException.class,()->codec.open(format.decode(bytes),snapshot,43L,key));
        byte[] corrupted=bytes.clone();corrupted[corrupted.length-1]^=1;
        assertThrows(SecurityException.class,()->codec.open(format.decode(corrupted),snapshot,42L,key));
        assertThrows(IllegalArgumentException.class,()->format.decode(Arrays.copyOf(bytes,bytes.length-5)));
        assertThrows(IllegalArgumentException.class,()->format.decode(new byte[]{0,1,2,3,4,5,6,7}));
        byte[] trailed=Arrays.copyOf(bytes,bytes.length+1);
        assertThrows(IllegalArgumentException.class,()->format.decode(trailed));
    }
}
