package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.crypto.KeyGenerator;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceBackupLocalArtifactStoreTest {
    @TempDir Path temp;

    private byte[] archive(long family, UUID snapshot) throws Exception {
        var keygen=KeyGenerator.getInstance("AES");
        keygen.init(256);
        var codec=new ProtectiveBackupArtifactCodec();
        byte[] plain="{\"format\":\"test\",\"categories\":{}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new ProtectiveBackupPortableFile().encode(codec.seal(
                snapshot,family,UUID.randomUUID(),0L,"a".repeat(64),"V50",plain,keygen.generateKey()));
    }

    @Test void atomicFileStorageRejectsCrossFamilyOverwriteAndCorruptHash() throws Exception {
        var format=new ProtectiveBackupPortableFile();
        var store=new WorkspaceBackupLocalArtifactStore(temp.resolve("backup").toString(),format);
        UUID id=UUID.randomUUID();
        byte[] encrypted=archive(17L,id);
        var result=store.store(17L,id,encrypted);
        assertEquals(17L,result.workspaceId());
        assertEquals(encrypted.length,result.byteCount());
        assertArrayEquals(encrypted,store.read(17L,id,result.sha256()));
        assertThrows(SecurityException.class,()->store.store(18L,id,encrypted));
        assertThrows(IllegalStateException.class,()->store.store(17L,id,encrypted));
        assertThrows(IllegalStateException.class,()->store.read(18L,id,result.sha256()));
        assertThrows(SecurityException.class,()->store.read(17L,id,"b".repeat(64)));
        // Actual disk tampering is detected before the bytes leave storage.
        Path file=temp.resolve("backup").resolve("17").resolve(id+".fpbackup");
        byte[] tampered=Files.readAllBytes(file);
        tampered[tampered.length-1]^=1;
        Files.write(file,tampered,StandardOpenOption.TRUNCATE_EXISTING);
        assertThrows(SecurityException.class,()->store.read(17L,id,result.sha256()));
        assertTrue(store.deleteAuthorizedExpired(17L,id));
        assertFalse(store.deleteAuthorizedExpired(17L,id));
        assertFalse(Files.exists(file));
    }

    @Test void unconfiguredStoreAndSymlinkDirectoryFailClosed() throws Exception {
        var format=new ProtectiveBackupPortableFile();
        UUID id=UUID.randomUUID();
        byte[] encrypted=archive(3L,id);
        assertThrows(IllegalStateException.class,
                ()->new WorkspaceBackupLocalArtifactStore("",format).store(3L,id,encrypted));
        Path real=Files.createDirectory(temp.resolve("real"));
        Path link=temp.resolve("linked");
        try {Files.createSymbolicLink(link,real);}
        catch(UnsupportedOperationException | java.nio.file.FileSystemException ex) {return;}
        assertThrows(SecurityException.class,
                ()->new WorkspaceBackupLocalArtifactStore(link.toString(),format)
                        .store(3L,id,encrypted));
    }
    @Test void inventoryIsReadOnlyAndFindsCrashOrphanTempAndRegularArchive() throws Exception {
        var format=new ProtectiveBackupPortableFile();
        Path root=temp.resolve("inventory");
        var store=new WorkspaceBackupLocalArtifactStore(root.toString(),format);
        UUID id=UUID.randomUUID();
        byte[] file=archive(73L,id);
        store.store(73L,id,file);
        Path folder=root.resolve("73");
        Path orphan=folder.resolve(".upload-crash.tmp");
        Files.write(orphan,new byte[]{1,2,3});
        var items=store.inventory(73L,10);
        assertEquals(2,items.size());
        assertTrue(items.stream().anyMatch(e->id.equals(e.snapshotId()) &&
                e.regularFile() && e.byteCount()==file.length));
        assertTrue(items.stream().anyMatch(e->e.fileName().equals(".upload-crash.tmp") &&
                e.snapshotId()==null));
        assertThrows(IllegalStateException.class,()->store.inventory(73L,1));
        assertTrue(Files.exists(orphan));
        assertTrue(Files.exists(folder.resolve(id+".fpbackup")));
    }

    @Test void ciphertextIntegrityPreflightDetectsTamperingWithoutDecryptingOrDeleting() throws Exception {
        var format=new ProtectiveBackupPortableFile();
        Path root=temp.resolve("integrity");
        var store=new WorkspaceBackupLocalArtifactStore(root.toString(),format);
        UUID id=UUID.randomUUID();
        byte[] file=archive(64L,id);
        var stored=store.store(64L,id,file);
        store.verifyStoredCiphertext(64L,id,stored.sha256());
        Path artifact=root.resolve("64").resolve(id+".fpbackup");
        byte[] tampered=Files.readAllBytes(artifact);
        tampered[tampered.length-1]^=0x01;
        Files.write(artifact,tampered,StandardOpenOption.TRUNCATE_EXISTING);
        assertThrows(SecurityException.class,()->store.verifyStoredCiphertext(
                64L,id,stored.sha256()));
        assertTrue(Files.exists(artifact),"Integrity failure must not delete the encrypted archive");
    }

}
