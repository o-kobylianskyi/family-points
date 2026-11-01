package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.Gson
import com.olehkobylianskyi.familypoints.android.data.ProtectiveBackupMetadataRepository
import org.junit.Assert.*
import org.junit.Test

class ProtectiveBackupMetadataContractTest {
    @Test fun serverMetadataMustNotBePresentedAsAnExportedArchive() {
        val dto = Gson().fromJson(
            """{"snapshotId":"11223344-0000-4000-8000-000000000001",
                "workspaceId":14,"operationId":"11223344-0000-4000-8000-000000000002",
                "dataEpoch":4,"resetMode":"SELECTIVE","selectedCategories":["TASKS"],
                "sourceDigest":"abc","state":"REQUESTED","createdAt":"2026-11-01T10:00:00Z",
                "expiresAt":"2026-12-01T10:00:00Z",
                "restoreVerified":false,"executable":false}""",
            ProtectiveBackupMetadataDto::class.java
        )
        assertEquals(14L, dto.workspaceId)
        assertEquals("REQUESTED", dto.state)
        assertTrue(ProtectiveBackupMetadataRepository.isMetadataOnly(dto))
        assertFalse(dto.restoreVerified)
        assertFalse(dto.executable)
    }
}
