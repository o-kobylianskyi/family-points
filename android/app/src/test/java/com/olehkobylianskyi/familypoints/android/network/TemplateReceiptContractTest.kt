package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class TemplateReceiptContractTest {
    private val gson = Gson()

    @Test fun v47ReceiptPreservesOriginalSelectionAndCounts() {
        val json = """{"operationId":"00000000-0000-4000-8000-000000000001","replayed":true,
          "importResult":{"selectedCodes":["TASK_A","REWARD_B"],
          "alreadyImported":["TASK_A"],"toImport":["REWARD_B"]}}"""
        val receipt = gson.fromJson(json, TemplateImportReceiptDto::class.java)
        assertTrue(receipt.replayed)
        assertEquals(listOf("TASK_A","REWARD_B"), receipt.importResult.selectedCodes)
        assertEquals(listOf("TASK_A"), receipt.importResult.alreadyImported)
        assertEquals(listOf("REWARD_B"), receipt.importResult.toImport)
    }

    @Test fun v48ReceiptIsMetadataNotAnEntityDefinition() {
        val json = """{"operationId":"00000000-0000-4000-8000-000000000002",
            "templateId":123,"kind":"REWARD","createdDefinitionId":456,"replayed":false}"""
        val receipt = gson.fromJson(json, TemplateActivationReceiptDto::class.java)
        assertEquals(123L, receipt.templateId)
        assertEquals("REWARD", receipt.kind)
        assertEquals(456L, receipt.createdDefinitionId)
        assertFalse(receipt.replayed)
    }

    @Test fun setupReviewMustDistinguishPreviewFromActivation() {
        val json = """{"workspaceId":7,"dataEpoch":0,"revision":9,"setupStatus":"IN_PROGRESS",
          "selectedCount":0,"alreadyImportedCount":0,"toImportCount":0,
          "selectedCodes":[],"alreadyImported":[],"toImport":[],
          "requiresExplicitConfirmation":true,"createsActiveDefinitions":false}"""
        val review = gson.fromJson(json, WorkspaceSetupReviewDto::class.java)
        assertEquals(9L, review.revision)
        assertTrue(review.requiresExplicitConfirmation)
        assertFalse(review.createsActiveDefinitions)
        assertTrue(review.selectedCodes.isEmpty())
    }
}
