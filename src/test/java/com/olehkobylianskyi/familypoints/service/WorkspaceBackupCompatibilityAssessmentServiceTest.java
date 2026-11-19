package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceBackupCompatibilityAssessmentServiceTest {
    private final ProtectiveBackupImportInspector inspector=mock(ProtectiveBackupImportInspector.class);
    private final WorkspaceBackupCompatibilityAssessmentService service=
            new WorkspaceBackupCompatibilityAssessmentService(inspector);
    private static final UUID SNAP=UUID.fromString("11111111-2222-4333-8444-555555555555");
    private static final UUID OP=UUID.fromString("22222222-3333-4444-8555-666666666666");
    private final OffsetDateTime now=OffsetDateTime.parse("2026-11-18T20:00:00Z");

    private WorkspaceProtectiveBackupService.Backup metadata(String state,
            boolean verified,boolean executable,long family,OffsetDateTime expiry) {
        return new WorkspaceProtectiveBackupService.Backup(SNAP,family,OP,4,
                "SELECTIVE",List.of("POINT_LEDGER","TASK_DEFINITIONS"),
                "a".repeat(64),state,now.minusHours(1),expiry,verified,executable);
    }

    @Test void registryShowsMetadataOnlyEvenWhenVerifiedAndNeverEnablesRestore() {
        var item=service.registry(metadata("VERIFIED",true,false,10,now.plusHours(1)),10,now);
        assertEquals(WorkspaceBackupCompatibilityAssessmentService.Status.METADATA_ONLY,item.status());
        assertTrue(item.restoreVerified());
        assertFalse(item.executable());
        assertEquals(-1,item.categories().get("POINT_LEDGER"));
        assertEquals(List.of("POINT_LEDGER","TASK_DEFINITIONS"),List.copyOf(item.categories().keySet()));
        assertTrue(item.blockers().contains("RESTORE_EXECUTION_DISABLED"));
        assertTrue(item.blockers().contains("REGISTRY_NOT_AUTHENTICATED_SOURCE"));
        verifyNoInteractions(inspector);
    }

    @Test void registryRejectsForeignTenantAndExecutableMetadataWithoutInspectingBytes() {
        assertThrows(SecurityException.class,
                ()->service.registry(metadata("REQUESTED",false,false,10,now.plusDays(1)),11,now));
        assertThrows(SecurityException.class,
                ()->service.registry(metadata("VERIFIED",true,true,10,now.plusDays(1)),10,now));
        verifyNoInteractions(inspector);
    }

    @Test void registryExpiredAndUnverifiedStatesCarryStableCompatibilityBlockers() {
        var item=service.registry(metadata("REQUESTED",false,false,5,now.minusSeconds(1)),5,now);
        assertTrue(item.blockers().contains("BACKUP_REGISTRY_EXPIRED"));
        assertTrue(item.blockers().contains("BACKUP_REGISTRY_NOT_VERIFIED"));
        assertFalse(item.executable());
    }

    @Test void legacyV1StillUsesExistingInspectorAndCannotClaimV2RestoreAuthority() {
        byte[] sealed={1,2,3};
        var key=new javax.crypto.spec.SecretKeySpec(new byte[16],"AES");
        when(inspector.inspect(sealed,7,key)).thenReturn(
                new ProtectiveBackupImportInspector.Inspection(SNAP,7,2,"V52",
                        List.of("TASK_DEFINITIONS"),Map.of("TASK_DEFINITIONS",2),
                        false,List.of("IDENTITY_REBIND_REQUIRED:TASK_EXECUTIONS.member_id")));
        var result=service.inspectLegacyV1(sealed,7,key);
        assertEquals(WorkspaceBackupCompatibilityAssessmentService.SourceKind.PORTABLE_V1,result.kind());
        assertEquals(WorkspaceBackupCompatibilityAssessmentService.Status.BLOCKED,result.status());
        assertTrue(result.blockers().contains("LEGACY_V1_NATIVE_REPLAY_UNSUPPORTED"));
        assertTrue(result.blockers().contains("IDENTITY_REBIND_REQUIRED:TASK_EXECUTIONS.member_id"));
        assertFalse(result.restoreVerified());
        assertFalse(result.executable());
        verify(inspector).inspect(sealed,7,key);
        verifyNoMoreInteractions(inspector);
    }

    @Test void v2AuthenticatesEightCategoriesWithoutEscalatingToPermit() {
        byte[] sealed={4,5,6};
        var key=new javax.crypto.spec.SecretKeySpec(new byte[16],"AES");
        var counts=Map.of("FAMILY_ROLES",1,"FAMILY_MEMBERS",2,"POINT_TYPES",1,
                "TASK_DEFINITIONS",1,"REWARD_DEFINITIONS",0,"TASK_EXECUTIONS",1,
                "REWARD_PURCHASES",0,"POINT_LEDGER",1);
        when(inspector.inspectAncestryV2(sealed,7,key)).thenReturn(
                new ProtectiveBackupImportInspector.AncestryV2Inspection(
                        SNAP,7,3,"b".repeat(64),"c".repeat(64),1,2,counts,
                        List.of(1L),List.of(2L,3L),false));
        var result=service.inspectOriginalV2(sealed,7,key);
        assertEquals(WorkspaceBackupCompatibilityAssessmentService.Status.AUTHENTICATED_READ_ONLY,result.status());
        assertEquals(8,result.categories().size());
        assertTrue(result.blockers().contains("TARGET_ORIGINAL_OWNER_AND_ONE_USE_PERMIT_REQUIRED"));
        assertFalse(result.executable());
        assertFalse(result.restoreVerified());
        verify(inspector).inspectAncestryV2(sealed,7,key);
        verifyNoMoreInteractions(inspector);
    }

    @Test void v2RejectsUnexpectedPartialCategoryPayloadEvenWhenInspectorStubReturnsIt() {
        byte[] sealed={7,8};
        var key=new javax.crypto.spec.SecretKeySpec(new byte[16],"AES");
        when(inspector.inspectAncestryV2(sealed,7,key)).thenReturn(
                new ProtectiveBackupImportInspector.AncestryV2Inspection(
                        SNAP,7,0,"a".repeat(64),"b".repeat(64),0,0,
                        Map.of("FAMILY_MEMBERS",0),List.of(),List.of(),false));
        assertThrows(SecurityException.class,()->service.inspectOriginalV2(sealed,7,key));
    }

    @Test void versionSpecificMalformedInputsFailClosed() {
        var key=new javax.crypto.spec.SecretKeySpec(new byte[16],"AES");
        assertThrows(SecurityException.class,()->service.inspectOriginalV2(null,5,key));
        assertThrows(SecurityException.class,()->service.inspectLegacyV1(new byte[]{1},-1,key));
        verifyNoInteractions(inspector);
    }
}
