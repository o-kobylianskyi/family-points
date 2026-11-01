package com.olehkobylianskyi.familypoints.android.network

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class TemplateActivationRequestContractTest {
    @Test fun existingReviewedJsonRetainsNullablePriceWithoutCoercion() {
        val input = """{"title":"Game time","price":null,"defaultDurationMinutes":30}"""
        val request = JsonParser.parseString(input).asJsonObject
        assertTrue(request.get("price").isJsonNull)
        assertEquals(30, request.get("defaultDurationMinutes").asInt)
        assertFalse(request.has("pricePoints"))
    }

    @Test fun draftPayloadCannotBeMistakenForReceipt() {
        val request = JsonParser.parseString("""{"title":"Homework","rewardPoints":5}""").asJsonObject
        assertFalse(request.has("operationId"))
        assertFalse(request.has("createdDefinitionId"))
        assertEquals("Homework", request.get("title").asString)
    }
}
