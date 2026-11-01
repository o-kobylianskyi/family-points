package com.olehkobylianskyi.familypoints.service;

import org.junit.jupiter.api.Test;
import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProtectiveBackupImportInspectorTest {
    private final ProtectiveBackupArtifactCodec codec=new ProtectiveBackupArtifactCodec();
    private final ProtectiveBackupPortableFile files=new ProtectiveBackupPortableFile();
    private final ProtectiveBackupImportInspector inspector=new ProtectiveBackupImportInspector(files,codec);

    private static javax.crypto.SecretKey key() throws Exception {
        var generator=KeyGenerator.getInstance("AES");generator.init(256);return generator.generateKey();
    }
    private byte[] file(long workspace,String source,javax.crypto.SecretKey key) {
        var artifact=codec.seal(UUID.randomUUID(),workspace,UUID.randomUUID(),0L,
                "d".repeat(64),"V50",source.getBytes(StandardCharsets.UTF_8),key);
        return files.encode(artifact);
    }
    @Test void showsReadOnlyCountsButNeverClaimsRestorable() throws Exception {
        var key=key();
        byte[] data=file(42L,"""
                {"format":"family-points-export-v1","workspaceId":42,
                 "categories":{"PRIVATE_TEMPLATES":[{"id":1,"workspace_id":42},{"id":2,"workspace_id":42}],
                               "MEMBER_GROUPS":[]}}
                """,key);
        var result=inspector.inspect(data,42L,key);
        assertEquals(2,result.counts().get("PRIVATE_TEMPLATES"));
        assertEquals(0,result.counts().get("MEMBER_GROUPS"));
        assertFalse(result.restorable());
        assertTrue(result.blockers().contains("RESTORE_ENGINE_NOT_IMPLEMENTED"));
        assertThrows(SecurityException.class,()->inspector.inspect(data,43L,key));
        assertThrows(SecurityException.class,()->inspector.inspect(data,42L,key()));
    }
    @Test void rejectsDuplicateIdsAndCrossFamilyRowsEvenWhenGcmIsValid() throws Exception {
        var key=key();
        var invalid=java.util.List.of(
                """
                {"format":"family-points-export-v1","workspaceId":42,"categories":{"MEMBER_GROUPS":[{"id":7,"family_id":42},{"id":7,"family_id":42}]}}
                """,
                """
                {"format":"family-points-export-v1","workspaceId":42,"categories":{"MEMBER_GROUPS":[{"id":7,"family_id":43}]}}
                """,
                """
                {"format":"family-points-export-v1","workspaceId":42,"categories":{"PRIVATE_TEMPLATES":[{"id":2,"workspace_id":41}]}}
                """,
                """
                {"format":"family-points-export-v1","workspaceId":42,"categories":{"TASK_DEFINITIONS":[{"id":0,"family_id":42}]}}
                """);
        for(var payload:invalid) {
            byte[] bytes=file(42L,payload,key);
            assertThrows(RuntimeException.class,()->inspector.inspect(bytes,42L,key));
        }
    }

    @Test void acceptsMediaCompositeKeysAndRejectsDuplicateBindings() throws Exception {
        var key=key();
        byte[] valid=file(42L,"""
                {"format":"family-points-export-v1","workspaceId":42,
                 "categories":{"MEDIA_BINDINGS":[{"workspace_id":42,
                     "target_type":"WORKSPACE","target_id":42,"slot":"COVER"}]}}
                """,key);
        assertEquals(1,inspector.inspect(valid,42L,key).counts().get("MEDIA_BINDINGS"));
        byte[] duplicated=file(42L,"""
                {"format":"family-points-export-v1","workspaceId":42,
                 "categories":{"MEDIA_BINDINGS":[
                    {"workspace_id":42,"target_type":"WORKSPACE","target_id":42,"slot":"COVER"},
                    {"workspace_id":42,"target_type":"WORKSPACE","target_id":42,"slot":"COVER"}]}}
                """,key);
        assertThrows(IllegalArgumentException.class,
                ()->inspector.inspect(duplicated,42L,key));
    }

    @Test void reportsMissingScheduleParentAndDanglingMediaAssetWithoutRestore() throws Exception {
        var key=key();
        byte[] missingTask=file(42L,"""
                {"format":"family-points-export-v1","workspaceId":42,
                 "categories":{"TASK_SCHEDULE_SLOTS":[{"id":3,"task_definition_id":9}]}}
                """,key);
        var first=inspector.inspect(missingTask,42L,key);
        assertTrue(first.blockers().contains("MISSING_CATEGORY:TASK_DEFINITIONS"));
        assertFalse(first.restorable());

        byte[] danglingTask=file(42L,"""
                {"format":"family-points-export-v1","workspaceId":42,
                 "categories":{"TASK_DEFINITIONS":[{"id":5,"family_id":42}],
                 "TASK_SCHEDULE_SLOTS":[{"id":3,"task_definition_id":9}]}}
                """,key);
        assertTrue(inspector.inspect(danglingTask,42L,key).blockers()
                .contains("DANGLING_REFERENCE:TASK_SCHEDULE_SLOTS.task_definition_id"));
        byte[] missingMedia=file(42L,"""
                {"format":"family-points-export-v1","workspaceId":42,
                 "categories":{"MEDIA_BINDINGS":[{"workspace_id":42,"target_type":"WORKSPACE",
                 "target_id":42,"slot":"COVER","asset_id":"77cc92a5-7905-4396-975a-8a1387555730"}]}}
                """,key);
        assertTrue(inspector.inspect(missingMedia,42L,key).blockers()
                .contains("MISSING_CATEGORY:MEDIA_ASSETS"));
        byte[] suppliedMedia=file(42L,"""
                {"format":"family-points-export-v1","workspaceId":42,
                 "categories":{"MEDIA_ASSETS":[{"id":"77cc92a5-7905-4396-975a-8a1387555730",
                 "workspace_id":42}],
                 "MEDIA_BINDINGS":[{"workspace_id":42,"target_type":"WORKSPACE",
                 "target_id":42,"slot":"COVER","asset_id":"77cc92a5-7905-4396-975a-8a1387555730"}]}}
                """,key);
        var checked=inspector.inspect(suppliedMedia,42L,key);
        assertFalse(checked.blockers().contains("MISSING_CATEGORY:MEDIA_ASSETS"));
        assertFalse(checked.blockers().contains("DANGLING_REFERENCE:MEDIA_BINDINGS.asset_id"));
        assertFalse(checked.restorable());
    }

    @Test void unknownCategoriesAndUnsafeArchiveShapeFailClosed() throws Exception {
        var key=key();
        for(String payload:java.util.List.of(
                "{\"format\":\"family-points-export-v1\",\"workspaceId\":42,\"categories\":{\"user_accounts\":[]}}",
                "{\"format\":\"family-points-export-v1\",\"workspaceId\":42,\"categories\":{\"PRIVATE_TEMPLATES\":[42]}}",
                "{\"format\":\"family-points-export-v1\",\"workspaceId\":43,\"categories\":{}}",
                "{\"format\":\"family-points-export-v0\",\"workspaceId\":42,\"categories\":{}}",
                "{\"format\":\"family-points-export-v1\",\"workspaceId\":42,\"categories\":{},\"extra\":true}")) {
            byte[] bytes=file(42L,payload,key);
            assertThrows(IllegalArgumentException.class,()->inspector.inspect(bytes,42L,key),payload);
        }
    }
}
