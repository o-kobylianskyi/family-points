package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupArtifactStagingServiceTest {
    @TempDir Path temp;

    @BeforeEach void beginRepeatableReadFixture() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                java.sql.Connection.TRANSACTION_REPEATABLE_READ);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach void clearTransactionFixture() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }



    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile files=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=new ProtectiveBackupImportInspector(files,codec);

    private SecretKey key() throws Exception {
        var generator=KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    private byte[] archive(long family,UUID snapshot,UUID operation,long epoch,
            SecretKey key,String category) {
        String json="{\"format\":\"family-points-export-v1\",\"workspaceId\":"+family+
                ",\"categories\":{\""+category+"\":[]}}";
        return files.encode(codec.seal(snapshot,family,operation,epoch,
                "a".repeat(64),"V50",json.getBytes(StandardCharsets.UTF_8),key));
    }

    @Test void stagesAndAuthenticatesPortableFileButDoesNotPromoteVerified() throws Exception {
        var exporter=mock(WorkspacePortableBackupExporter.class);
        var storage=new WorkspaceBackupLocalArtifactStore(temp.resolve("storage").toString(),files);
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector,files);
        var key=key();
        UUID snapshot=UUID.randomUUID(),operation=UUID.randomUUID();
        byte[] encrypted=archive(31,snapshot,operation,0,key,"MEMBER_GROUPS");
        when(exporter.export(eq(31L),eq(snapshot),eq(operation),eq(0L),
                eq(new TreeSet<>(Set.of("MEMBER_GROUPS"))),eq("V50"),same(key)))
                .thenReturn(new WorkspacePortableBackupExporter.Result(
                        encrypted.clone(),"a".repeat(64),files.decode(encrypted).manifest().contentBytes(),snapshot,Map.of("MEMBER_GROUPS",0)));
        var result=staging.stage(31,snapshot,operation,0,
                List.of("MEMBER_GROUPS"),"V50",key);
        assertEquals("STORED_UNVERIFIED",result.state());
        assertEquals(31,result.workspaceId());
        assertEquals(Map.of("MEMBER_GROUPS",0),result.counts());
        assertEquals(result.storedBytes(),storage.read(31,snapshot,result.storageSha256()).length);
        assertThrows(IllegalStateException.class,()->storage.store(31,snapshot,
                archive(31,snapshot,operation,0,key,"MEMBER_GROUPS")));
        verify(exporter,times(1)).export(anyLong(),any(),any(),anyLong(),any(),any(),any());
    }

    @Test void rejectsForgedExporterScopeBeforeWritingFile() throws Exception {
        var exporter=mock(WorkspacePortableBackupExporter.class);
        var storage=new WorkspaceBackupLocalArtifactStore(temp.resolve("storage").toString(),files);
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector,files);
        var key=key();
        UUID snapshot=UUID.randomUUID(),operation=UUID.randomUUID();
        byte[] encrypted=archive(32,snapshot,operation,0,key,"MEMBER_GROUPS");
        when(exporter.export(anyLong(),any(),any(),anyLong(),any(),any(),any()))
                .thenReturn(new WorkspacePortableBackupExporter.Result(
                        encrypted,"a".repeat(64),files.decode(encrypted).manifest().contentBytes(),snapshot,Map.of("MEMBER_GROUPS",0)));
        assertThrows(SecurityException.class,()->staging.stage(31,snapshot,operation,0,
                List.of("MEMBER_GROUPS"),"V50",key));
        assertThrows(IllegalStateException.class,()->storage.read(31,snapshot,"a".repeat(64)));
    }

    @Test void rejectsDifferentOperationAndDifferentSourceDigestBeforeDiskPublication() throws Exception {
        for(int variant=0;variant<3;variant++) {
            var exporter=mock(WorkspacePortableBackupExporter.class);
            var storage=new WorkspaceBackupLocalArtifactStore(
                    temp.resolve("mismatch"+variant).toString(),files);
            var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector,files);
            var key=key();
            UUID snapshot=UUID.randomUUID(),operation=UUID.randomUUID();
            UUID encodedOperation=variant==0?UUID.randomUUID():operation;
            byte[] encrypted=archive(41,snapshot,encodedOperation,0,key,"MEMBER_GROUPS");
            String digest=variant==1?"b".repeat(64):"a".repeat(64);
            long size=files.decode(encrypted).manifest().contentBytes();
            if(variant==2)size++;
            when(exporter.export(anyLong(),any(),any(),anyLong(),any(),any(),any()))
                    .thenReturn(new WorkspacePortableBackupExporter.Result(
                            encrypted,digest,size,snapshot,Map.of("MEMBER_GROUPS",0)));
            assertThrows(SecurityException.class,()->staging.stage(41,snapshot,operation,0,
                    List.of("MEMBER_GROUPS"),"V50",key));
            assertFalse(java.nio.file.Files.exists(
                    temp.resolve("mismatch"+variant).resolve("41")
                            .resolve(snapshot+".fpbackup")));
        }
    }

    @Test void invalidDuplicateCategoriesNeverReachExporter() throws Exception {
        var exporter=mock(WorkspacePortableBackupExporter.class);
        var storage=new WorkspaceBackupLocalArtifactStore(temp.resolve("store").toString(),files);
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector,files);
        assertThrows(IllegalArgumentException.class,()->staging.stage(1,
                UUID.randomUUID(),UUID.randomUUID(),0,
                List.of("MEMBER_GROUPS","MEMBER_GROUPS"),"V50",key()));
        verifyNoInteractions(exporter);
    }

    @Test void directStageRejectsReadCommittedBeforeExporterOrDisk() throws Exception {
        var exporter=mock(WorkspacePortableBackupExporter.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector,files);
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                java.sql.Connection.TRANSACTION_READ_COMMITTED);
        var rejected=assertThrows(IllegalStateException.class,()->staging.stage(
                31,UUID.randomUUID(),UUID.randomUUID(),0,List.of("MEMBER_GROUPS"),"V50",key()));
        assertEquals("BACKUP_SNAPSHOT_ISOLATION_REQUIRED",rejected.getMessage());
        verifyNoInteractions(exporter,storage);
    }

    @Test void directStageRejectsUnknownIsolationBeforeExporterOrDisk() throws Exception {
        var exporter=mock(WorkspacePortableBackupExporter.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector,files);
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
        var rejected=assertThrows(IllegalStateException.class,()->staging.stage(
                31,UUID.randomUUID(),UUID.randomUUID(),0,List.of("MEMBER_GROUPS"),"V50",key()));
        assertEquals("BACKUP_SNAPSHOT_ISOLATION_REQUIRED",rejected.getMessage());
        verifyNoInteractions(exporter,storage);
    }

    @Test void directStageRejectsAbsentTransactionBeforeExporterOrDisk() throws Exception {
        var exporter=mock(WorkspacePortableBackupExporter.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector,files);
        TransactionSynchronizationManager.setActualTransactionActive(false);
        var rejected=assertThrows(IllegalStateException.class,()->staging.stage(
                31,UUID.randomUUID(),UUID.randomUUID(),0,List.of("MEMBER_GROUPS"),"V50",key()));
        assertEquals("BACKUP_SNAPSHOT_ISOLATION_REQUIRED",rejected.getMessage());
        verifyNoInteractions(exporter,storage);
    }

}
