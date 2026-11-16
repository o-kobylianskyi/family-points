package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
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
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector);
        var key=key();
        UUID snapshot=UUID.randomUUID(),operation=UUID.randomUUID();
        byte[] encrypted=archive(31,snapshot,operation,0,key,"MEMBER_GROUPS");
        when(exporter.export(eq(31L),eq(snapshot),eq(operation),eq(0L),
                eq(new TreeSet<>(Set.of("MEMBER_GROUPS"))),eq("V50"),same(key)))
                .thenReturn(new WorkspacePortableBackupExporter.Result(
                        encrypted.clone(),"a".repeat(64),31,snapshot,Map.of("MEMBER_GROUPS",0)));
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
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector);
        var key=key();
        UUID snapshot=UUID.randomUUID(),operation=UUID.randomUUID();
        byte[] encrypted=archive(32,snapshot,operation,0,key,"MEMBER_GROUPS");
        when(exporter.export(anyLong(),any(),any(),anyLong(),any(),any(),any()))
                .thenReturn(new WorkspacePortableBackupExporter.Result(
                        encrypted,"a".repeat(64),31,snapshot,Map.of("MEMBER_GROUPS",0)));
        assertThrows(SecurityException.class,()->staging.stage(31,snapshot,operation,0,
                List.of("MEMBER_GROUPS"),"V50",key));
        assertThrows(IllegalStateException.class,()->storage.read(31,snapshot,"a".repeat(64)));
    }

    @Test void invalidDuplicateCategoriesNeverReachExporter() throws Exception {
        var exporter=mock(WorkspacePortableBackupExporter.class);
        var storage=new WorkspaceBackupLocalArtifactStore(temp.resolve("store").toString(),files);
        var staging=new WorkspaceBackupArtifactStagingService(exporter,storage,inspector);
        assertThrows(IllegalArgumentException.class,()->staging.stage(1,
                UUID.randomUUID(),UUID.randomUUID(),0,
                List.of("MEMBER_GROUPS","MEMBER_GROUPS"),"V50",key()));
        verifyNoInteractions(exporter);
    }
}
