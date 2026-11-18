package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupOrphanAuditServiceTest {
    @Test void reportsAbsentRegistryAndSuspiciousTempWithoutDeletingEither() {
        var db=mock(JdbcTemplate.class);
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        long workspace=19L;
        UUID id=UUID.randomUUID();
        when(store.inventory(workspace,20)).thenReturn(List.of(
                new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                        id+".fpbackup",id,115L,true,false),
                new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                        ".upload-crashed.tmp",null,64L,true,false)));
        when(db.queryForList(anyString(),eq(workspace),eq(id)))
                .thenReturn(Collections.emptyList());
        var audit=new WorkspaceBackupOrphanAuditService(db,store);
        var results=audit.audit(workspace,20);
        assertEquals(2,results.size());
        assertEquals("NO_REGISTRY_RECORD",results.get(0).finding());
        assertEquals("UNRECOGNIZED_OR_UNSAFE_FILE",results.get(1).finding());
        verify(store,never()).deleteAuthorizedExpired(anyLong(),any());
    }

    @Test void registeredAndExpiredFilesAreDistinctAndNeverAutoRemoved() {
        var db=mock(JdbcTemplate.class);
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        UUID valid=UUID.randomUUID(),expired=UUID.randomUUID();
        when(store.inventory(22L,10)).thenReturn(List.of(
                new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                        valid+".fpbackup",valid,128L,true,false),
                new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                        expired+".fpbackup",expired,64L,true,false)));
        when(db.queryForList(anyString(),eq(22L),eq(valid)))
                .thenReturn(List.of(Map.of("state","VERIFIED",
                        "storage_key","local:v1:22/"+valid+".fpbackup",
                        "artifact_sha256","a".repeat(64))));
        when(db.queryForList(anyString(),eq(22L),eq(expired)))
                .thenReturn(List.of(Map.of("state","EXPIRED",
                        "storage_key","local:v1:22/"+expired+".fpbackup",
                        "artifact_sha256","b".repeat(64))));
        var results=new WorkspaceBackupOrphanAuditService(db,store).audit(22L,10);
        assertEquals("REGISTERED_FILE_PRESENT",results.get(0).finding());
        assertEquals("EXPIRED_REGISTRY_FILE_PRESENT",results.get(1).finding());
        verify(store,never()).deleteAuthorizedExpired(anyLong(),any());
    }
}
