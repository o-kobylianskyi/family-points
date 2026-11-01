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
                 "categories":{"PRIVATE_TEMPLATES":[{"id":1},{"id":2}],
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
                "{\\"format\\":\\"family-points-export-v1\\",\\"workspaceId\\":42,\\"categories\\":{\\"MEMBER_GROUPS\\":[{\\"id\\":7,\\"family_id\\":42},{\\"id\\":7,\\"family_id\\":42}]}}",
                "{\\"format\\":\\"family-points-export-v1\\",\\"workspaceId\\":42,\\"categories\\":{\\"MEMBER_GROUPS\\":[{\\"id\\":7,\\"family_id\\":43}]}}",
                "{\\"format\\":\\"family-points-export-v1\\",\\"workspaceId\\":42,\\"categories\\":{\\"PRIVATE_TEMPLATES\\":[{\\"id\\":2,\\"workspace_id\\":41}]}}",
                "{\\"format\\":\\"family-points-export-v1\\",\\"workspaceId\\":42,\\"categories\\":{\\"TASK_DEFINITIONS\\":[{\\"id\\":0,\\"family_id\\":42}]}}");
        for(var payload:invalid) {
            byte[] bytes=file(42L,payload,key);
            assertThrows(RuntimeException.class,()->inspector.inspect(bytes,42L,key));
        }
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
