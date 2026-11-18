package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupReconciliationAssessmentServiceTest {
    private static final Instant NOW=Instant.parse("2026-11-18T10:00:00Z");
    private static final Clock CLOCK=Clock.fixed(NOW,ZoneOffset.UTC);
    private static final Instant OLD=NOW.minusSeconds(26*3600L);

    private static WorkspaceBackupLocalArtifactStore.InventoryEntry entry(
            String name,UUID id,Instant time) {
        return new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                name,id,128L,true,false,time);
    }

    @Test void crashRemnantAndUnregisteredArtifactAreReviewOnlyNeverDeleteAuthorized() {
        var db=mock(JdbcTemplate.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        UUID id=UUID.randomUUID();
        when(storage.inventory(7L,20)).thenReturn(List.of(
                entry(".upload-crashed.tmp",null,OLD),
                entry(id+".fpbackup",id,OLD)));
        when(db.queryForList(anyString(),eq(7L),eq(id))).thenReturn(List.of());
        var result=new WorkspaceBackupReconciliationAssessmentService(
                db,storage,CLOCK).assess(7L,20);
        assertEquals("STALE_TEMP_UPLOAD_REVIEW_ONLY",result.get(0).classification());
        assertEquals("UNREGISTERED_FILE_REVIEW_ONLY",result.get(1).classification());
        assertTrue(result.stream().noneMatch(
                WorkspaceBackupReconciliationAssessmentService.Assessment::deleteAuthorized));
        verify(storage,never()).deleteAuthorizedExpired(anyLong(),any());
        verify(db,never()).update(anyString(),any(Object[].class));
    }

    @Test void activeLeaseWinsOverExpiredRegistryStateAndDoesNotPromoteDeletion() {
        var db=mock(JdbcTemplate.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        UUID leased=UUID.randomUUID(),inProgress=UUID.randomUUID(),expired=UUID.randomUUID();
        when(storage.inventory(11L,10)).thenReturn(List.of(
                entry(leased+".fpbackup",leased,OLD),
                entry(inProgress+".fpbackup",inProgress,OLD),
                entry(expired+".fpbackup",expired,OLD)));
        when(db.queryForList(anyString(),eq(11L),eq(leased))).thenReturn(
                List.of(Map.of("state","EXPIRED","active_lease",true)));
        when(db.queryForList(anyString(),eq(11L),eq(inProgress))).thenReturn(
                List.of(Map.of("state","BACKING_UP","active_lease",false)));
        when(db.queryForList(anyString(),eq(11L),eq(expired))).thenReturn(
                List.of(Map.of("state","EXPIRED","active_lease",false)));
        var result=new WorkspaceBackupReconciliationAssessmentService(
                db,storage,CLOCK).assess(11L,10);
        assertEquals("ACTIVE_RESTORE_LEASE_HOLD",result.get(0).classification());
        assertTrue(result.get(0).hasActiveLease());
        assertEquals("IN_PROGRESS_REGISTRY_HOLD",result.get(1).classification());
        assertEquals("REGISTRY_OWNED_FILE_HOLD",result.get(2).classification());
        assertTrue(result.stream().noneMatch(
                WorkspaceBackupReconciliationAssessmentService.Assessment::deleteAuthorized));
        verify(storage,never()).deleteAuthorizedExpired(anyLong(),any());
    }

    @Test void recentlyPublishedFutureTimestampUnknownAgeAndMaliciousNamesFailClosed() {
        var db=mock(JdbcTemplate.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        UUID id=UUID.randomUUID();
        when(storage.inventory(33L,20)).thenReturn(List.of(
                entry(id+".fpbackup",id,NOW.minusSeconds(30)),
                entry(".upload-future.tmp",null,NOW.plusSeconds(20)),
                entry("notes.txt",null,OLD),
                entry(".upload-unknown.tmp",null,null),
                new WorkspaceBackupLocalArtifactStore.InventoryEntry(
                        ".upload-symlink.tmp",null,-1L,false,true,OLD)));
        var results=new WorkspaceBackupReconciliationAssessmentService(
                db,storage,CLOCK).assess(33L,20);
        assertEquals(List.of("RECENT_FILE_HOLD","FUTURE_FILE_TIMESTAMP_HOLD",
                        "UNRECOGNIZED_FILE_HOLD","UNKNOWN_FILE_AGE_HOLD","UNSAFE_FILE_HOLD"),
                results.stream().map(
                        WorkspaceBackupReconciliationAssessmentService.Assessment::classification).toList());
        verifyNoInteractions(db);
        verify(storage,never()).deleteAuthorizedExpired(anyLong(),any());
    }

    @Test void hardFileLimitPropagatesAndBadArgumentsCannotScan() {
        var db=mock(JdbcTemplate.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var scanner=new WorkspaceBackupReconciliationAssessmentService(db,storage,CLOCK);
        assertThrows(IllegalArgumentException.class,()->scanner.assess(0,20));
        assertThrows(IllegalArgumentException.class,()->scanner.assess(7,1001));
        verifyNoInteractions(db,storage);
        when(storage.inventory(7L,2)).thenThrow(
                new IllegalStateException("BACKUP_INVENTORY_LIMIT_EXCEEDED"));
        assertEquals("BACKUP_INVENTORY_LIMIT_EXCEEDED",
                assertThrows(IllegalStateException.class,()->scanner.assess(7,2)).getMessage());
        verify(storage,never()).deleteAuthorizedExpired(anyLong(),any());
    }
}
