package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.crypto.KeyGenerator;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupRegistryStagingCoordinatorTest {
    @Test void neverClaimsRecoveryVerificationEvenWhenEncryptedFileIsStaged() throws Exception {
        var registry=mock(WorkspaceProtectiveBackupService.class);
        var stage=mock(WorkspaceBackupArtifactStagingService.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var coordinator=new WorkspaceBackupRegistryStagingCoordinator(registry,stage,storage);
        UUID operation=UUID.randomUUID(),snapshot=UUID.randomUUID();
        var request=new WorkspaceProtectiveBackupService.Request(operation,"SELECTIVE",
                List.of("MEMBER_GROUPS"),"a".repeat(64),"V50");
        var backup=new WorkspaceProtectiveBackupService.Backup(snapshot,7L,operation,0L,
                "SELECTIVE",List.of("MEMBER_GROUPS"),"a".repeat(64),"REQUESTED",
                OffsetDateTime.now(),OffsetDateTime.now().plusDays(30),false,false);
        var staged=new WorkspaceBackupArtifactStagingService.Staged(
                snapshot,7L,"a".repeat(64),"b".repeat(64),198,
                Map.of("MEMBER_GROUPS",0),"STORED_UNVERIFIED");
        when(registry.request(7,3,request)).thenReturn(backup);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        when(stage.stage(7L,snapshot,operation,0L,List.of("MEMBER_GROUPS"),"V50",key))
                .thenReturn(staged);
        beginSynchronization();
        try {
            var result=coordinator.registerAndStage(7,3,request,0,key);
            assertEquals(snapshot,result.registryEntry().snapshotId());
            assertFalse(result.recoveryVerified());
            assertFalse(result.registryEntry().restoreVerified());
            verify(stage,times(1)).stage(7L,snapshot,operation,0L,List.of("MEMBER_GROUPS"),"V50",key);
            verify(registry).recordStaged(7L,snapshot,operation,"a".repeat(64),"b".repeat(64),198);
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    sync->sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(storage).deleteAuthorizedExpired(7L,snapshot);
        } finally {endSynchronization();}
    }

    @Test void rejectedRegistryStateCannotStageAnyBytes() throws Exception {
        var registry=mock(WorkspaceProtectiveBackupService.class);
        var stage=mock(WorkspaceBackupArtifactStagingService.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var coordinator=new WorkspaceBackupRegistryStagingCoordinator(registry,stage,storage);
        UUID operation=UUID.randomUUID(),snapshot=UUID.randomUUID();
        var request=new WorkspaceProtectiveBackupService.Request(operation,"SELECTIVE",
                List.of("MEMBER_GROUPS"),"a".repeat(64),"V50");
        var backup=new WorkspaceProtectiveBackupService.Backup(snapshot,7L,operation,0L,
                "SELECTIVE",List.of("MEMBER_GROUPS"),"a".repeat(64),"VERIFIED",
                OffsetDateTime.now(),OffsetDateTime.now().plusDays(30),true,false);
        when(registry.request(7,3,request)).thenReturn(backup);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        beginSynchronization();
        try {
            assertThrows(IllegalStateException.class,()->coordinator.registerAndStage(
                    7,3,request,0,generator.generateKey()));
            verifyNoInteractions(stage,storage);
            verify(registry,never()).recordStaged(anyLong(),any(),any(),any(),any(),anyInt());
        } finally {endSynchronization();}
    }
    @Test void committedStagingDoesNotDeleteEncryptedArtifact() throws Exception {
        var registry=mock(WorkspaceProtectiveBackupService.class);
        var stage=mock(WorkspaceBackupArtifactStagingService.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var coordinator=new WorkspaceBackupRegistryStagingCoordinator(registry,stage,storage);
        UUID operation=UUID.randomUUID(),snapshot=UUID.randomUUID();
        var request=new WorkspaceProtectiveBackupService.Request(operation,"SELECTIVE",
                List.of("MEMBER_GROUPS"),"a".repeat(64),"V50");
        var backup=new WorkspaceProtectiveBackupService.Backup(snapshot,7L,operation,0L,
                "SELECTIVE",List.of("MEMBER_GROUPS"),"a".repeat(64),"REQUESTED",
                OffsetDateTime.now(),OffsetDateTime.now().plusDays(30),false,false);
        var staged=new WorkspaceBackupArtifactStagingService.Staged(
                snapshot,7L,"a".repeat(64),"b".repeat(64),198,
                Map.of("MEMBER_GROUPS",0),"STORED_UNVERIFIED");
        when(registry.request(7,3,request)).thenReturn(backup);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var key=generator.generateKey();
        when(stage.stage(7L,snapshot,operation,0L,List.of("MEMBER_GROUPS"),"V50",key))
                .thenReturn(staged);
        beginSynchronization();
        try {
            coordinator.registerAndStage(7,3,request,0,key);
            verify(registry).recordStaged(7L,snapshot,operation,"a".repeat(64),"b".repeat(64),198);
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    sync->sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            verifyNoInteractions(storage);
        } finally {endSynchronization();}
    }

    @Test void stagingMustRunInsideActiveTransaction() throws Exception {
        var registry=mock(WorkspaceProtectiveBackupService.class);
        var stage=mock(WorkspaceBackupArtifactStagingService.class);
        var storage=mock(WorkspaceBackupLocalArtifactStore.class);
        var coordinator=new WorkspaceBackupRegistryStagingCoordinator(registry,stage,storage);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        var request=new WorkspaceProtectiveBackupService.Request(
                UUID.randomUUID(),"SELECTIVE",List.of("MEMBER_GROUPS"),"a".repeat(64),"V50");
        assertThrows(IllegalStateException.class,()->coordinator.registerAndStage(
                7,3,request,0,generator.generateKey()));
        verifyNoInteractions(registry,stage,storage);
    }

    private static void beginSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private static void endSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

}
