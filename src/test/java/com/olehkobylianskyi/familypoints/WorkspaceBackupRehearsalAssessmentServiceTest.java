package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.KeyGenerator;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupRehearsalAssessmentServiceTest {
    private static final List<String> SCOPE=List.of(
            "MEDIA_ASSETS","MEDIA_BINDINGS","MEMBER_GROUPS");

    private static Map<String,Object> registry(long workspace,UUID snapshot,String state) {
        var row=new HashMap<String,Object>();
        row.put("snapshot_id",snapshot);
        row.put("workspace_id",workspace);
        row.put("operation_id",UUID.randomUUID());
        row.put("source_digest","a".repeat(64));
        row.put("state",state);
        row.put("encrypted",true);
        row.put("storage_key","local:v1:"+workspace+"/"+snapshot+".fpbackup");
        row.put("artifact_sha256","b".repeat(64));
        row.put("artifact_bytes",4);
        row.put("selected_categories_json",
                "[\"MEDIA_ASSETS\",\"MEDIA_BINDINGS\",\"MEMBER_GROUPS\"]");
        row.put("expires_at",OffsetDateTime.now().plusDays(5));
        return row;
    }

    @Test void stagedEncryptedFileRehearsesButNeverBecomesVerified() throws Exception {
        var db=mock(JdbcTemplate.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var inspector=mock(ProtectiveBackupImportInspector.class);
        var rehearsal=mock(WorkspaceDisposableRestoreRehearsal.class);
        var service=new WorkspaceBackupRehearsalAssessmentService(db,storage,inspector,rehearsal);
        UUID snapshot=UUID.randomUUID();
        when(db.queryForList(anyString(),eq(7L),eq(snapshot)))
                .thenReturn(List.of(registry(7L,snapshot,"BACKING_UP")));
        byte[] file=new byte[]{1,2,3,4};
        when(storage.read(7L,snapshot,"b".repeat(64))).thenReturn(file);
        var keyGen=KeyGenerator.getInstance("AES");keyGen.init(256);
        var key=keyGen.generateKey();
        when(inspector.inspect(file,7L,key)).thenReturn(
                new ProtectiveBackupImportInspector.Inspection(snapshot,7L,0L,
                        "V51",SCOPE,Map.of("MEMBER_GROUPS",1,
                                "MEDIA_ASSETS",1,"MEDIA_BINDINGS",1),false,List.of()));
        when(rehearsal.rehearse(file,7L,key)).thenReturn(
                new WorkspaceDisposableRestoreRehearsal.Report(7L,snapshot,1,1,1,2,true));
        var result=service.assess(7L,snapshot,key);
        assertEquals("ROLLBACK_ONLY_REHEARSED",result.evidenceState());
        assertFalse(result.verifiedForRestore());
        assertEquals(2,result.mediaVariants());
        verify(rehearsal).rehearse(file,7L,key);
        assertArrayEquals(new byte[]{0,0,0,0},file);
    }

    @Test void unsupportedScopeAndExpiredOrVerifiedRegistryNeverReadFiles() throws Exception {
        var db=mock(JdbcTemplate.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var inspector=mock(ProtectiveBackupImportInspector.class);
        var rehearsal=mock(WorkspaceDisposableRestoreRehearsal.class);
        var service=new WorkspaceBackupRehearsalAssessmentService(db,storage,inspector,rehearsal);
        UUID snapshot=UUID.randomUUID();
        var keyGen=KeyGenerator.getInstance("AES");keyGen.init(256);
        var key=keyGen.generateKey();
        for(int scenario=0;scenario<3;scenario++) {
            var row=registry(7L,snapshot,scenario==0?"VERIFIED":"BACKING_UP");
            if(scenario==1)row.put("expires_at",OffsetDateTime.now().minusSeconds(10));
            if(scenario==2)row.put("selected_categories_json","[\"MEMBER_GROUPS\"]");
            when(db.queryForList(anyString(),eq(7L),eq(snapshot)))
                    .thenReturn(List.of(row));
            assertThrows(SecurityException.class,()->service.assess(7L,snapshot,key));
        }
        verifyNoInteractions(storage,inspector,rehearsal);
    }

    @Test void mismatchCountsAndMissingFileFailClosedWithoutPromotion() throws Exception {
        var db=mock(JdbcTemplate.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var inspector=mock(ProtectiveBackupImportInspector.class);
        var rehearsal=mock(WorkspaceDisposableRestoreRehearsal.class);
        var service=new WorkspaceBackupRehearsalAssessmentService(db,storage,inspector,rehearsal);
        UUID snapshot=UUID.randomUUID();
        when(db.queryForList(anyString(),eq(7L),eq(snapshot)))
                .thenReturn(List.of(registry(7L,snapshot,"BACKING_UP")));
        var keyGen=KeyGenerator.getInstance("AES");keyGen.init(256);
        var key=keyGen.generateKey();
        when(storage.read(7L,snapshot,"b".repeat(64)))
                .thenThrow(new SecurityException("BACKUP_STORAGE_HASH_MISMATCH"));
        assertThrows(SecurityException.class,()->service.assess(7L,snapshot,key));
        verifyNoInteractions(inspector,rehearsal);
    }
}
