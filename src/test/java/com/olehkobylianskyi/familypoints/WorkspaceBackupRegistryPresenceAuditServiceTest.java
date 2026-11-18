package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupRegistryPresenceAuditServiceTest {
    @Test void classifiesNotStagedMissingCorruptAndIntactWithoutMutation() {
        var db=mock(JdbcTemplate.class);
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        UUID unstaged=UUID.randomUUID(),missing=UUID.randomUUID(),
                corrupt=UUID.randomUUID(),intact=UUID.randomUUID();
        when(db.queryForList(anyString(),eq(8L),eq(5))).thenReturn(List.of(
                Map.of("snapshot_id",unstaged,"state","REQUESTED"),
                record(missing,8L,"VERIFIED","a".repeat(64)),
                record(corrupt,8L,"BLOCKED","b".repeat(64)),
                record(intact,8L,"EXPIRED","c".repeat(64))));
        doThrow(new IllegalStateException("BACKUP_LOCAL_FOLDER_MISSING"))
                .when(store).verifyStoredCiphertext(8L,missing,"a".repeat(64));
        doThrow(new SecurityException("BACKUP_STORAGE_HASH_MISMATCH"))
                .when(store).verifyStoredCiphertext(8L,corrupt,"b".repeat(64));
        when(db.queryForList(anyString(),eq(8L),eq(intact))).thenReturn(
                List.of(record(intact,8L,"EXPIRED","c".repeat(64))));
        var findings=new WorkspaceBackupRegistryPresenceAuditService(db,store).audit(8L,4);
        assertEquals(List.of("NOT_STAGED_IN_REGISTRY","REGISTRY_CIPHERTEXT_UNAVAILABLE",
                "REGISTRY_CIPHERTEXT_INVALID","REGISTRY_CIPHERTEXT_PRESENT"),
                findings.stream().map(
                        WorkspaceBackupRegistryPresenceAuditService.Finding::classification).toList());
        assertEquals("EXPIRED",findings.get(3).registryState());
        verify(store,never()).deleteAuthorizedExpired(anyLong(),any());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void malformedMetadataAndOverflowFailClosedWithoutDisablingRetention() {
        var db=mock(JdbcTemplate.class);
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        UUID broken=UUID.randomUUID();
        when(db.queryForList(anyString(),eq(17L),eq(2))).thenReturn(List.of(
                Map.of("snapshot_id",broken,"state","BACKING_UP",
                        "storage_key","local:v1:999/wrong.fpbackup",
                        "artifact_sha256","bad")));
        var service=new WorkspaceBackupRegistryPresenceAuditService(db,store);
        assertEquals("INCOMPLETE_REGISTRY_EVIDENCE",
                service.audit(17L,1).get(0).classification());
        verifyNoInteractions(store);
        assertThrows(IllegalArgumentException.class,()->service.audit(17L,0));
        assertThrows(IllegalArgumentException.class,()->service.audit(17L,1001));

        when(db.queryForList(anyString(),eq(17L),eq(2))).thenReturn(
                List.of(Map.of("snapshot_id",UUID.randomUUID(),"state","REQUESTED"),
                        Map.of("snapshot_id",UUID.randomUUID(),"state","REQUESTED")));
        assertEquals("BACKUP_REGISTRY_AUDIT_LIMIT_EXCEEDED",
                assertThrows(IllegalStateException.class,()->service.audit(17L,1)).getMessage());
        verify(store,never()).deleteAuthorizedExpired(anyLong(),any());
    }

    private static Map<String,Object> record(UUID id,long workspace,String state,String sha) {
        return Map.of("snapshot_id",id,"state",state,
                "storage_key","local:v1:"+workspace+"/"+id+".fpbackup",
                "artifact_sha256",sha);
    }
    @Test void changedRegistryDuringCiphertextCheckIsNotTreatedAsStable() {
        var db=mock(JdbcTemplate.class);
        var store=mock(WorkspaceBackupLocalArtifactStore.class);
        UUID id=UUID.randomUUID();
        when(db.queryForList(anyString(),eq(42L),eq(2))).thenReturn(
                List.of(record(id,42L,"BACKING_UP","a".repeat(64))));
        when(db.queryForList(anyString(),eq(42L),eq(id))).thenReturn(
                List.of(record(id,42L,"BLOCKED","b".repeat(64))));
        var result=new WorkspaceBackupRegistryPresenceAuditService(db,store).audit(42L,1);
        assertEquals("REGISTRY_CHANGED_DURING_AUDIT",result.get(0).classification());
        verify(store).verifyStoredCiphertext(42L,id,"a".repeat(64));
        verify(store,never()).deleteAuthorizedExpired(anyLong(),any());
    }

}
