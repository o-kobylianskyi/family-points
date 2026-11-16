package com.olehkobylianskyi.familypoints;

import com.olehkobylianskyi.familypoints.service.*;
import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupRegistryStagingCoordinatorTest {
    @Test void neverClaimsRecoveryVerificationEvenWhenEncryptedFileIsStaged() throws Exception {
        var registry=mock(WorkspaceProtectiveBackupService.class);
        var stage=mock(WorkspaceBackupArtifactStagingService.class);
        var coordinator=new WorkspaceBackupRegistryStagingCoordinator(registry,stage);
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
        var result=coordinator.registerAndStage(7,3,request,0,key);
        assertEquals(snapshot,result.registryEntry().snapshotId());
        assertFalse(result.recoveryVerified());
        assertFalse(result.registryEntry().restoreVerified());
        verify(stage,times(1)).stage(7L,snapshot,operation,0L,List.of("MEMBER_GROUPS"),"V50",key);
    }

    @Test void rejectedRegistryStateCannotStageAnyBytes() throws Exception {
        var registry=mock(WorkspaceProtectiveBackupService.class);
        var stage=mock(WorkspaceBackupArtifactStagingService.class);
        var coordinator=new WorkspaceBackupRegistryStagingCoordinator(registry,stage);
        UUID operation=UUID.randomUUID(),snapshot=UUID.randomUUID();
        var request=new WorkspaceProtectiveBackupService.Request(operation,"SELECTIVE",
                List.of("MEMBER_GROUPS"),"a".repeat(64),"V50");
        var backup=new WorkspaceProtectiveBackupService.Backup(snapshot,7L,operation,0L,
                "SELECTIVE",List.of("MEMBER_GROUPS"),"a".repeat(64),"VERIFIED",
                OffsetDateTime.now(),OffsetDateTime.now().plusDays(30),true,false);
        when(registry.request(7,3,request)).thenReturn(backup);
        var generator=KeyGenerator.getInstance("AES");generator.init(256);
        assertThrows(IllegalStateException.class,()->coordinator.registerAndStage(
                7,3,request,0,generator.generateKey()));
        verifyNoInteractions(stage);
    }
}
