package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class SetupFinalizeReceiptContractTest {
    private val gson = Gson()
    @Test fun v49ReceiptHasExactWorkspaceGenerationAndPrivateImportOnly() {
        val receipt = gson.fromJson(
            """{"operationId":"00000000-0000-4000-8000-000000000007","replayed":true,
            "workspaceId":19,"dataEpoch":3,"revision":8,"setupStatus":"COMPLETED",
            "importResult":{"selectedCodes":["TASK_1","REWARD_1"],
            "alreadyImported":["TASK_1"],"toImport":["REWARD_1"]}}""",
            SetupFinalizeImportReceiptDto::class.java)
        assertEquals(19L, receipt.workspaceId)
        assertEquals(3L, receipt.dataEpoch)
        assertEquals(8L, receipt.revision)
        assertEquals("COMPLETED", receipt.setupStatus)
        assertTrue(receipt.replayed)
        assertEquals(listOf("TASK_1", "REWARD_1"), receipt.importResult.selectedCodes)
    }
    @Test fun requestPreservesOrderedSelectionAndEmptyExplicitChoice() {
        val request = SetupFinalizeImportRequest(7L, listOf("B", "A"))
        assertEquals("""{"expectedRevision":7,"selectedCatalogCodes":["B","A"]}""", gson.toJson(request))
        assertEquals(emptyList<String>(), SetupFinalizeImportRequest(1L, emptyList()).selectedCatalogCodes)
    }
}
